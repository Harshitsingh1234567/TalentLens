package com.resumeanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.entity.JobDescription;
import com.resumeanalyzer.repository.AnalysisRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingEntryRepository;
import com.resumeanalyzer.repository.ScreeningBatchRankingRepository;
import com.resumeanalyzer.repository.JobDescriptionRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.repository.ScreeningBatchRepository;
import com.resumeanalyzer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for POST /api/job-descriptions, exercising the full
 * stack behind real JWT authentication issued by the Phase 1 auth
 * endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JobDescriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ScreeningBatchRepository screeningBatchRepository;

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
        // Analysis is deleted first since it references Resume/JobDescription.
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
    void submittingValidJobDescriptionCreatesRecordOwnedByAuthenticatedUser() throws Exception {
        String token = registerAndLogin("recruiter-jd@example.com", "SecurePass123", "RECRUITER");
        Map<String, Object> payload = Map.of("content", "We are looking for a senior backend engineer with Java and Spring Boot experience.");

        String responseBody = mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("We are looking for a senior backend engineer with Java and Spring Boot experience."))
                .andReturn().getResponse().getContentAsString();

        Long jdId = ((Number) objectMapper.readValue(responseBody, Map.class).get("id")).longValue();
        Optional<JobDescription> saved = jobDescriptionRepository.findByIdFetchingOwner(jdId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getOwner().getEmail()).isEqualTo("recruiter-jd@example.com");
    }

    @Test
    void submittingBlankContentIsRejected() throws Exception {
        String token = registerAndLogin("jobseeker-jd@example.com", "SecurePass123", "JOB_SEEKER");
        Map<String, Object> payload = Map.of("content", "   ");

        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void submittingWithoutJwtIsRejected() throws Exception {
        Map<String, Object> payload = Map.of("content", "Some job description text.");

        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listingJobDescriptionsReturnsOnlyAuthenticatedUsersOwnJobDescriptions() throws Exception {
        String token = registerAndLogin("jd-lister@example.com", "SecurePass123", "JOB_SEEKER");
        submitJobDescription(token, "First job description content.");
        // A content long enough to exercise the truncated-preview path.
        String longContent = "Backend engineer role. ".repeat(10).trim();
        submitJobDescription(token, longContent);

        // Assertions are deliberately order-independent: submittedAt is a
        // wall-clock Instant, and these two lightweight JSON submissions
        // can land in the same millisecond, so a strict $[0]/$[1] index
        // assertion would be flaky rather than meaningfully verifying
        // anything this endpoint promises.
        mockMvc.perform(get("/api/job-descriptions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[*].submittedAt", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[*].contentPreview")
                        .value(org.hamcrest.Matchers.hasItem("First job description content.")))
                .andExpect(jsonPath("$[*].contentPreview")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.endsWith("\u2026"),
                                org.hamcrest.Matchers.hasLength(141)))));
    }

    @Test
    void listingJobDescriptionsDoesNotReturnAnotherUsersJobDescriptions() throws Exception {
        String ownerToken = registerAndLogin("jd-owner@example.com", "SecurePass123", "JOB_SEEKER");
        String otherToken = registerAndLogin("jd-other-user@example.com", "SecurePass123", "JOB_SEEKER");
        submitJobDescription(ownerToken, "Owner's job description.");

        mockMvc.perform(get("/api/job-descriptions").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listingJobDescriptionsReturnsEmptyListWhenUserHasNoJobDescriptions() throws Exception {
        String token = registerAndLogin("no-jds@example.com", "SecurePass123", "JOB_SEEKER");

        mockMvc.perform(get("/api/job-descriptions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listingJobDescriptionsWithoutJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/job-descriptions"))
                .andExpect(status().isUnauthorized());
    }

    private void submitJobDescription(String token, String content) throws Exception {
        Map<String, Object> payload = Map.of("content", content);
        mockMvc.perform(post("/api/job-descriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
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
}
