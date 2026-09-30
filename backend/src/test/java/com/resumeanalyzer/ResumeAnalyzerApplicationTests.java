package com.resumeanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context smoke test, introduced in Phase 0 and unchanged in Phase 1.
 *
 * It verifies that the entire Spring application context - JPA, the real
 * Phase 1 JWT-based Spring Security configuration, JwtProperties/
 * MatchingWeightsProperties configuration binding, and the authentication
 * service/controller beans - wires together and starts successfully.
 *
 * This test runs against the H2 in-memory database defined in
 * src/test/resources/application.yml so it does not require an external
 * MySQL server to be running.
 */
@SpringBootTest
class ResumeAnalyzerApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a successful context load is the assertion.
    }
}
