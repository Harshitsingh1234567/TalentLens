package com.resumeanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingEntryRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingRepository;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for POST/GET /api/screening-batches (Phase 6),
 * exercising the full stack (controller -> service -> real PDFBox/POI
 * parsing -> repository -> H2 test database) behind real JWT
 * authentication issued by the Phase 1 auth endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScreeningBatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ScreeningBatchRepository screeningBatchRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private ScreeningBatchRankingEntryRepository rankingEntryRepository;

    @Autowired
    private ScreeningBatchRankingRepository rankingRepository;

    @BeforeEach
    void cleanDatabase() {
        // All @SpringBootTest classes in this project share one cached
        // Spring context (and H2 database instance) across the test run,
        // so all child tables must be cleared before deleting users.
        // Resume references ScreeningBatch, so it is cleared first.
        // Phase 7: ranking rows reference Resume/ScreeningBatch/JobDescription
        // and must be cleared before those parent tables.
        rankingEntryRepository.deleteAll();
        rankingRepository.deleteAll();
        analysisRepository.deleteAll();
        resumeRepository.deleteAll();
        screeningBatchRepository.deleteAll();
        jobDescriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void creatingABatchAsRecruiterPersistsBatchAndAssociatesResumes() throws Exception {
        String token = registerAndLogin("recruiter-create@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile pdf = new MockMultipartFile(
                "files", "alice_resume.pdf", "application/pdf", buildPdf("Experienced Java backend engineer."));
        MockMultipartFile docx = new MockMultipartFile(
                "files", "bob_resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                buildDocx("Skilled frontend developer with React expertise."));

        String responseBody = mockMvc.perform(multipart("/api/screening-batches")
                        .file(pdf)
                        .file(docx)
                        .param("name", "Backend Engineer - Sept 2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Backend Engineer - Sept 2026"))
                .andExpect(jsonPath("$.resumes.length()").value(2))
                .andExpect(jsonPath("$.failedFiles").isEmpty())
                .andExpect(jsonPath("$.resumes[0].candidateName").value("alice_resume"))
                .andExpect(jsonPath("$.resumes[1].candidateName").value("bob_resume"))
                .andReturn().getResponse().getContentAsString();

        Long batchId = ((Number) objectMapper.readValue(responseBody, Map.class).get("id")).longValue();
        List<Resume> persisted = resumeRepository.findByScreeningBatchIdOrderByUploadedAtAsc(batchId);
        assertThat(persisted).hasSize(2);
        assertThat(persisted).allMatch(r -> r.getScreeningBatch().getId().equals(batchId));
    }

    @Test
    void creatingABatchReportsPerFileFailuresWithoutLosingValidFiles() throws Exception {
        String token = registerAndLogin("recruiter-partial@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile good = new MockMultipartFile("files", "good.pdf", "application/pdf", buildPdf("Valid resume text."));
        MockMultipartFile corrupt = new MockMultipartFile("files", "corrupt.pdf", "application/pdf", "not a real pdf".getBytes());

        mockMvc.perform(multipart("/api/screening-batches")
                        .file(good)
                        .file(corrupt)
                        .param("name", "Batch with one bad file")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumes.length()").value(1))
                .andExpect(jsonPath("$.resumes[0].originalFilename").value("good.pdf"))
                .andExpect(jsonPath("$.failedFiles.length()").value(1))
                .andExpect(jsonPath("$.failedFiles[0].originalFilename").value("corrupt.pdf"))
                .andExpect(jsonPath("$.failedFiles[0].reason").exists());
    }

    @Test
    void creatingABatchAllowsDuplicateFilesInTheSameRequest() throws Exception {
        String token = registerAndLogin("recruiter-dup@example.com", "SecurePass123", "RECRUITER");
        byte[] pdfBytes = buildPdf("Identical content.");
        MockMultipartFile file1 = new MockMultipartFile("files", "same_name.pdf", "application/pdf", pdfBytes);
        MockMultipartFile file2 = new MockMultipartFile("files", "same_name.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart("/api/screening-batches")
                        .file(file1)
                        .file(file2)
                        .param("name", "Duplicate name batch")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumes.length()").value(2));
    }

    @Test
    void creatingABatchAsJobSeekerIsForbidden() throws Exception {
        String token = registerAndLogin("jobseeker-batch@example.com", "SecurePass123", "JOB_SEEKER");
        MockMultipartFile file = new MockMultipartFile("files", "resume.pdf", "application/pdf", buildPdf("text"));

        mockMvc.perform(multipart("/api/screening-batches")
                        .file(file)
                        .param("name", "Should not be allowed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(screeningBatchRepository.count()).isZero();
    }

    @Test
    void creatingABatchWithoutJwtIsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "resume.pdf", "application/pdf", buildPdf("text"));

        mockMvc.perform(multipart("/api/screening-batches")
                        .file(file)
                        .param("name", "No auth"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingABatchWithBlankNameIsRejected() throws Exception {
        String token = registerAndLogin("recruiter-blankname@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile file = new MockMultipartFile("files", "resume.pdf", "application/pdf", buildPdf("text"));

        mockMvc.perform(multipart("/api/screening-batches")
                        .file(file)
                        .param("name", "   ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void creatingABatchWithNoFilesIsRejected() throws Exception {
        String token = registerAndLogin("recruiter-nofiles@example.com", "SecurePass123", "RECRUITER");

        mockMvc.perform(multipart("/api/screening-batches")
                        .param("name", "Empty batch attempt")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        assertThat(screeningBatchRepository.count()).isZero();
    }

    @Test
    void retrievingABatchAsOwningRecruiterReturnsItsResumes() throws Exception {
        String token = registerAndLogin("recruiter-get@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile file = new MockMultipartFile("files", "carol.pdf", "application/pdf", buildPdf("Carol's resume text."));

        String createResponse = mockMvc.perform(multipart("/api/screening-batches")
                        .file(file)
                        .param("name", "Retrieve me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long batchId = ((Number) objectMapper.readValue(createResponse, Map.class).get("id")).longValue();

        mockMvc.perform(get("/api/screening-batches/" + batchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(batchId))
                .andExpect(jsonPath("$.name").value("Retrieve me"))
                .andExpect(jsonPath("$.resumes.length()").value(1))
                .andExpect(jsonPath("$.resumes[0].originalFilename").value("carol.pdf"));
    }

    @Test
    void retrievingAnotherRecruitersBatchIsRejectedWithNotFound() throws Exception {
        String ownerToken = registerAndLogin("owner-recruiter@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile file = new MockMultipartFile("files", "resume.pdf", "application/pdf", buildPdf("text"));
        String createResponse = mockMvc.perform(multipart("/api/screening-batches")
                        .file(file)
                        .param("name", "Owner's batch")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long batchId = ((Number) objectMapper.readValue(createResponse, Map.class).get("id")).longValue();

        String attackerToken = registerAndLogin("attacker-recruiter@example.com", "SecurePass123", "RECRUITER");

        mockMvc.perform(get("/api/screening-batches/" + batchId)
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void retrievingABatchAsJobSeekerIsForbidden() throws Exception {
        String recruiterToken = registerAndLogin("recruiter-for-forbid@example.com", "SecurePass123", "RECRUITER");
        MockMultipartFile file = new MockMultipartFile("files", "resume.pdf", "application/pdf", buildPdf("text"));
        String createResponse = mockMvc.perform(multipart("/api/screening-batches")
                        .file(file)
                        .param("name", "Recruiter's batch")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long batchId = ((Number) objectMapper.readValue(createResponse, Map.class).get("id")).longValue();

        String jobSeekerToken = registerAndLogin("jobseeker-for-forbid@example.com", "SecurePass123", "JOB_SEEKER");

        mockMvc.perform(get("/api/screening-batches/" + batchId)
                        .header("Authorization", "Bearer " + jobSeekerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void retrievingANonExistentBatchReturnsNotFound() throws Exception {
        String token = registerAndLogin("recruiter-missing@example.com", "SecurePass123", "RECRUITER");

        mockMvc.perform(get("/api/screening-batches/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void retrievingABatchWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/screening-batches/1"))
                .andExpect(status().isUnauthorized());
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

    private byte[] buildDocx(String text) throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            run.setText(text);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        }
    }
}
