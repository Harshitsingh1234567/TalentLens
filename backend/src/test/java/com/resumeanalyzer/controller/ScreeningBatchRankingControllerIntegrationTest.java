package com.resumeanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingEntryRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingRepository;
import com.resumeanalyzer.repository.ScreeningBatchRepository;
import com.resumeanalyzer.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for POST /api/screening-batches/{id}/analyze and
 * GET /api/screening-batches/{id}/ranking (Phase 7), exercising the full
 * stack (controller -> service -> real MatchScoringEngine ->
 * repositories -> H2 test database) behind real JWT authentication,
 * Recruiter-only access, and screening-batch/job-description ownership
 * enforcement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScreeningBatchRankingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScreeningBatchRankingEntryRepository rankingEntryRepository;

    @Autowired
    private ScreeningBatchRankingRepository rankingRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ScreeningBatchRepository screeningBatchRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        // Children before parents: ranking entries -> rankings -> resumes/batches -> job descriptions -> users.
        rankingEntryRepository.deleteAll();
        rankingRepository.deleteAll();
        resumeRepository.deleteAll();
        screeningBatchRepository.deleteAll();
        jobDescriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void analyzingABatchRanksCandidatesHighestScoreFirst() throws Exception {
        String token = registerAndLogin("recruiter-analyze@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(token, "Backend hiring", Map.of(
                "weak.pdf", "Knows only HTML and CSS.",
                "strong.pdf", "Senior Java Spring Boot engineer with Docker and AWS experience."));
        Long jdId = submitJobDescription(token, "Looking for a Java Spring Boot engineer with AWS experience.");

        Map<String, Object> payload = Map.of("jobDescriptionId", jdId);

        mockMvc.perform(post("/api/screening-batches/" + batchId + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.screeningBatchId").value(batchId))
                .andExpect(jsonPath("$.jobDescriptionId").value(jdId))
                .andExpect(jsonPath("$.candidatesRanked").value(2))
                .andExpect(jsonPath("$.rankings.length()").value(2))
                .andExpect(jsonPath("$.rankings[0].rank").value(1))
                .andExpect(jsonPath("$.rankings[0].candidateName").value("strong"))
                .andExpect(jsonPath("$.rankings[0].finalScore").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.rankings[0].matchedSkills", hasItem("java")))
                .andExpect(jsonPath("$.rankings[1].candidateName").value("weak"));

        // GET reflects exactly what was just analyzed.
        mockMvc.perform(get("/api/screening-batches/" + batchId + "/ranking")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatesRanked").value(2))
                .andExpect(jsonPath("$.rankings[0].candidateName").value("strong"));
    }

    @Test
    void reanalyzingABatchReplacesThePreviousRanking() throws Exception {
        String token = registerAndLogin("recruiter-reanalyze@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(token, "Reanalyze batch", Map.of("candidate.pdf", "Java developer resume."));
        Long firstJdId = submitJobDescription(token, "Java role.");
        Long secondJdId = submitJobDescription(token, "Python role.");
        Long thirdJdId = submitJobDescription(token, "DevOps role.");

        // Regression test for the re-analysis persistence-ordering defect:
        // deleting the previous ranking (header + entries) and inserting
        // the new one happen in the same transaction, against a header
        // table with a one-ranking-per-batch unique constraint. Before the
        // fix, the second (and any subsequent) call failed with HTTP 500
        // (unique constraint violation) because the delete had not been
        // physically flushed before the insert. Three consecutive
        // successful calls, each replacing the last, demonstrate the fix
        // holds up beyond just a single re-analysis.
        analyze(token, batchId, firstJdId).andExpect(status().isCreated());
        analyze(token, batchId, secondJdId).andExpect(status().isCreated());
        analyze(token, batchId, thirdJdId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobDescriptionId").value(thirdJdId))
                .andExpect(jsonPath("$.rankings.length()").value(1));

        mockMvc.perform(get("/api/screening-batches/" + batchId + "/ranking")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobDescriptionId").value(thirdJdId))
                .andExpect(jsonPath("$.rankings.length()").value(1));

        // Exactly one ranking (and one ranked entry) persists for the
        // batch - no duplicate/orphaned rows left behind by the earlier
        // replaced rankings.
        assertEquals(1, rankingRepository.findAll().stream()
                .filter(r -> r.getScreeningBatch().getId().equals(batchId))
                .count());
        assertEquals(1, rankingEntryRepository.findAll().size());
    }

    @Test
    void analyzingAnEmptyBatchProducesAZeroCandidateRankingNotAnError() throws Exception {
        String token = registerAndLogin("recruiter-empty@example.com", "SecurePass123", "RECRUITER");
        // A batch create request requires at least one file, so instead we
        // exercise the "no successfully processed resumes" case: every
        // uploaded file fails to parse (unsupported extension).
        Long batchId = createBatchWithUnparseableFile(token, "All rejected batch");
        Long jdId = submitJobDescription(token, "Any role.");

        analyze(token, batchId, jdId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidatesRanked").value(0))
                .andExpect(jsonPath("$.rankings.length()").value(0));

        mockMvc.perform(get("/api/screening-batches/" + batchId + "/ranking")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatesRanked").value(0))
                .andExpect(jsonPath("$.rankings").isEmpty());
    }

    @Test
    void gettingRankingBeforeAnyAnalysisReturnsNotFound() throws Exception {
        String token = registerAndLogin("recruiter-never-analyzed@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(token, "Never analyzed", Map.of("resume.pdf", "Some text."));

        mockMvc.perform(get("/api/screening-batches/" + batchId + "/ranking")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void analyzingAsJobSeekerIsForbidden() throws Exception {
        String recruiterToken = registerAndLogin("recruiter-for-forbid@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(recruiterToken, "Recruiter's batch", Map.of("resume.pdf", "Java developer."));
        Long jdId = submitJobDescription(recruiterToken, "Java role.");

        String jobSeekerToken = registerAndLogin("jobseeker-for-forbid@example.com", "SecurePass123", "JOB_SEEKER");

        analyze(jobSeekerToken, batchId, jdId).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/screening-batches/" + batchId + "/ranking")
                        .header("Authorization", "Bearer " + jobSeekerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyzingAnotherRecruitersBatchIsRejectedWithNotFound() throws Exception {
        String ownerToken = registerAndLogin("owner-recruiter@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(ownerToken, "Owner's batch", Map.of("resume.pdf", "Java developer."));
        Long ownerJdId = submitJobDescription(ownerToken, "Java role.");

        String attackerToken = registerAndLogin("attacker-recruiter@example.com", "SecurePass123", "RECRUITER");

        analyze(attackerToken, batchId, ownerJdId).andExpect(status().isNotFound());
    }

    @Test
    void analyzingWithAnotherRecruitersJobDescriptionIsRejectedWithNotFound() throws Exception {
        String ownerToken = registerAndLogin("owner-recruiter-2@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(ownerToken, "Owner's batch", Map.of("resume.pdf", "Java developer."));

        String otherToken = registerAndLogin("other-recruiter-2@example.com", "SecurePass123", "RECRUITER");
        Long foreignJdId = submitJobDescription(otherToken, "Some role.");

        analyze(ownerToken, batchId, foreignJdId).andExpect(status().isNotFound());
    }

    @Test
    void analyzingANonExistentBatchReturnsNotFound() throws Exception {
        String token = registerAndLogin("recruiter-missing-batch@example.com", "SecurePass123", "RECRUITER");
        Long jdId = submitJobDescription(token, "Java role.");

        analyze(token, 999999L, jdId).andExpect(status().isNotFound());
    }

    @Test
    void analyzingWithANonExistentJobDescriptionReturnsNotFound() throws Exception {
        String token = registerAndLogin("recruiter-missing-jd@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(token, "Batch", Map.of("resume.pdf", "Java developer."));

        analyze(token, batchId, 999999L).andExpect(status().isNotFound());
    }

    @Test
    void analyzingWithMissingJobDescriptionIdIsRejected() throws Exception {
        String token = registerAndLogin("recruiter-validation@example.com", "SecurePass123", "RECRUITER");
        Long batchId = createBatch(token, "Batch", Map.of("resume.pdf", "Java developer."));

        mockMvc.perform(post("/api/screening-batches/" + batchId + "/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.jobDescriptionId").exists());
    }

    @Test
    void analyzingWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/screening-batches/1/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("jobDescriptionId", 1L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gettingRankingWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/screening-batches/1/ranking"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions analyze(String token, Long batchId, Long jdId) throws Exception {
        Map<String, Object> payload = Map.of("jobDescriptionId", jdId);
        return mockMvc.perform(post("/api/screening-batches/" + batchId + "/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .header("Authorization", "Bearer " + token));
    }

    private String registerAndLogin(String email, String password, String role) throws Exception {
        Map<String, Object> registerPayload = Map.of("email", email, "password", password, "role", role);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        Map<String, Object> loginPayload = Map.of("email", email, "password", password);
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return (String) objectMapper.readValue(loginResponse, Map.class).get("token");
    }

    private Long submitJobDescription(String token, String content) throws Exception {
        String response = mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();
    }

    /**
     * Creates a screening batch whose PDF files carry the given
     * (filename without extension -> text content) entries - filename is
     * used as the candidate name, so tests can assert on it directly.
     */
    private Long createBatch(String token, String name, Map<String, String> filenameToText) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart("/api/screening-batches");
        for (Map.Entry<String, String> entry : filenameToText.entrySet()) {
            builder.file(new MockMultipartFile("files", entry.getKey(), "application/pdf", buildPdf(entry.getValue())));
        }
        String response = mockMvc.perform(builder.param("name", name).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();
    }

    private Long createBatchWithUnparseableFile(String token, String name) throws Exception {
        MockMultipartFile unsupportedFile =
                new MockMultipartFile("files", "resume.txt", "text/plain", "plain text is unsupported".getBytes());

        String response = mockMvc.perform(multipart("/api/screening-batches")
                        .file(unsupportedFile)
                        .param("name", name)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumes.length()").value(0))
                .andExpect(jsonPath("$.failedFiles.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        return ((Number) objectMapper.readValue(response, Map.class).get("id")).longValue();
    }

    private byte[] buildPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
