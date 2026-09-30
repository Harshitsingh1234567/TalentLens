package com.resumeanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for POST /api/matches, exercising the full stack
 * (controller -> service -> real MatchScoringEngine -> repositories -> H2
 * test database) behind real JWT authentication and ownership enforcement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MatchControllerIntegrationTest {

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
        // Analysis is deleted first since it references Resume/JobDescription/User.
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
    void matchingOwnedResumeAndJobDescriptionReturnsScoreBreakdown() throws Exception {
        String token = registerAndLogin("matcher@example.com", "SecurePass123", "JOB_SEEKER");

        Long resumeId = uploadResume(token, "Experienced Java and Spring Boot backend engineer with Docker skills.");
        Long jdId = submitJobDescription(token, "Seeking a Java and Spring Boot engineer with AWS experience.");

        Map<String, Object> payload = Map.of("resumeId", resumeId, "jobDescriptionId", jdId);

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value(resumeId))
                .andExpect(jsonPath("$.jobDescriptionId").value(jdId))
                .andExpect(jsonPath("$.matchedSkills", org.hamcrest.Matchers.hasItem("java")))
                .andExpect(jsonPath("$.missingSkills", org.hamcrest.Matchers.hasItem("aws")))
                .andExpect(jsonPath("$.skillScore").exists())
                .andExpect(jsonPath("$.experienceScore").exists())
                .andExpect(jsonPath("$.semanticScore").exists())
                .andExpect(jsonPath("$.finalScore").exists())
                .andExpect(jsonPath("$.explanation").exists());
    }

    @Test
    void matchingAnotherUsersResumeIsRejectedWithNotFound() throws Exception {
        String ownerToken = registerAndLogin("owner@example.com", "SecurePass123", "JOB_SEEKER");
        Long resumeId = uploadResume(ownerToken, "Java developer resume content.");

        String attackerToken = registerAndLogin("attacker@example.com", "SecurePass123", "JOB_SEEKER");
        Long attackerJdId = submitJobDescription(attackerToken, "Java engineer wanted.");

        Map<String, Object> payload = Map.of("resumeId", resumeId, "jobDescriptionId", attackerJdId);

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void matchingWithoutJwtIsRejected() throws Exception {
        Map<String, Object> payload = Map.of("resumeId", 1L, "jobDescriptionId", 2L);

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void matchingWithMissingIdsIsRejected() throws Exception {
        String token = registerAndLogin("validation@example.com", "SecurePass123", "JOB_SEEKER");
        Map<String, Object> payload = Map.of();

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.resumeId").exists())
                .andExpect(jsonPath("$.fieldErrors.jobDescriptionId").exists());
    }

    @Test
    void matchingNonExistentResumeIsRejectedWithNotFound() throws Exception {
        String token = registerAndLogin("nonexistent@example.com", "SecurePass123", "JOB_SEEKER");
        Long jdId = submitJobDescription(token, "Java engineer wanted.");

        Map<String, Object> payload = Map.of("resumeId", 999999L, "jobDescriptionId", jdId);

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private Long uploadResume(String token, String resumeText) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", buildPdf(resumeText));

        String responseBody = mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) objectMapper.readValue(responseBody, Map.class).get("id")).longValue();
    }

    private Long submitJobDescription(String token, String content) throws Exception {
        Map<String, Object> payload = Map.of("content", content);

        String responseBody = mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) objectMapper.readValue(responseBody, Map.class).get("id")).longValue();
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
}
