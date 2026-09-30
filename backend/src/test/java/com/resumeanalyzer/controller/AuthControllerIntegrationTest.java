package com.resumeanalyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.entity.User;
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
 * End-to-end tests for POST /api/auth/register and POST /api/auth/login,
 * exercising the full stack (controller -> service -> security ->
 * repository -> H2 test database) exactly as a real client would.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ScreeningBatchRepository screeningBatchRepository;

    @Autowired
    private JobDescriptionRepository jobDescriptionRepository;

    @Autowired
    private AnalysisRepository analysisRepository;

    @Autowired
    private ScreeningBatchRankingEntryRepository rankingEntryRepository;

    @Autowired
    private ScreeningBatchRankingRepository rankingRepository;

    @BeforeEach
    void cleanDatabase() {
        // Resume, JobDescription, and Analysis hold foreign keys to User
        // (and Analysis also to Resume/JobDescription). All @SpringBootTest
        // classes in this project share one cached Spring context (and
        // therefore one H2 database instance) across the whole test run,
        // so child rows created by a different test class must be cleared
        // first, or deleting users here could fail with a foreign key
        // violation. Analysis is deleted first since it references both
        // Resume and JobDescription.
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
    void registerWithValidPayloadCreatesAccountAndNeverReturnsPassword() throws Exception {
        Map<String, Object> payload = Map.of(
                "email", "jobseeker@example.com",
                "password", "SecurePass123",
                "role", "JOB_SEEKER"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jobseeker@example.com"))
                .andExpect(jsonPath("$.role").value("JOB_SEEKER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void duplicateEmailRegistrationIsRejectedWithConflict() throws Exception {
        Map<String, Object> payload = Map.of(
                "email", "duplicate@example.com",
                "password", "SecurePass123",
                "role", "RECRUITER"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Same email again - must be rejected.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void passwordIsStoredAsBCryptHashRatherThanPlaintext() throws Exception {
        Map<String, Object> payload = Map.of(
                "email", "hash-check@example.com",
                "password", "SecurePass123",
                "role", "JOB_SEEKER"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        Optional<User> saved = userRepository.findByEmail("hash-check@example.com");
        assertThat(saved).isPresent();
        String storedPassword = saved.get().getPassword();

        assertThat(storedPassword).isNotEqualTo("SecurePass123");
        // BCrypt hashes always start with one of these version prefixes.
        assertThat(storedPassword).matches("^\\$2[aby]\\$.{56}$");
    }

    @Test
    void loginWithCorrectCredentialsReturnsJwt() throws Exception {
        registerUser("login-success@example.com", "SecurePass123", "JOB_SEEKER");

        Map<String, Object> loginPayload = Map.of(
                "email", "login-success@example.com",
                "password", "SecurePass123"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("login-success@example.com"))
                .andExpect(jsonPath("$.role").value("JOB_SEEKER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginWithWrongPasswordIsRejectedWithGenericMessage() throws Exception {
        registerUser("wrong-password@example.com", "SecurePass123", "JOB_SEEKER");

        Map<String, Object> loginPayload = Map.of(
                "email", "wrong-password@example.com",
                "password", "IncorrectPassword"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginWithUnknownEmailIsRejectedWithSameGenericMessageAsWrongPassword() throws Exception {
        Map<String, Object> loginPayload = Map.of(
                "email", "no-such-account@example.com",
                "password", "WhateverPassword1"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isUnauthorized())
                // Identical message to the wrong-password case above proves
                // the API does not leak whether the email is registered.
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void registrationValidationFailsForMalformedEmailAndShortPassword() throws Exception {
        Map<String, Object> payload = Map.of(
                "email", "not-an-email",
                "password", "short",
                "role", "JOB_SEEKER"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void protectedEndpointWithoutJwtIsRejected() throws Exception {
        // No business endpoints exist yet in Phase 1, so this exercises the
        // "deny by default" rule itself: any path other than the two public
        // auth endpoints must require authentication, enforced by the
        // security filter chain before any controller is reached.
        mockMvc.perform(get("/api/some-protected-resource"))
                .andExpect(status().isUnauthorized());
    }

    private void registerUser(String email, String password, String role) throws Exception {
        Map<String, Object> payload = Map.of(
                "email", email,
                "password", password,
                "role", role
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }
}
