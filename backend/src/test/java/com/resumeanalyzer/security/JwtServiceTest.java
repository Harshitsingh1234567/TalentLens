package com.resumeanalyzer.security;

import com.resumeanalyzer.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies JWT generation and validation behavior in isolation, without
 * needing a running Spring context or a database.
 */
class JwtServiceTest {

    private static final String TEST_SECRET =
            "unit-test-fixed-secret-key-not-used-anywhere-else-1234567890";

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpirationMs(60_000L); // 1 minute, plenty for a fast test
        jwtService = new JwtService(jwtProperties);
    }

    @Test
    void generatesTokenContainingSubjectAndRoleClaim() {
        String token = jwtService.generateToken("jobseeker@example.com", Role.JOB_SEEKER);

        assertEquals("jobseeker@example.com", jwtService.extractEmail(token));
        assertEquals("JOB_SEEKER", jwtService.extractRole(token));
    }

    @Test
    void validTokenIsAcceptedForMatchingUser() {
        String token = jwtService.generateToken("recruiter@example.com", Role.RECRUITER);
        UserDetails userDetails = User.withUsername("recruiter@example.com")
                .password("irrelevant-hash")
                .authorities("ROLE_RECRUITER")
                .build();

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void tokenIsRejectedWhenSubjectDoesNotMatchUserDetails() {
        String token = jwtService.generateToken("someone@example.com", Role.JOB_SEEKER);
        UserDetails otherUser = User.withUsername("someone-else@example.com")
                .password("irrelevant-hash")
                .authorities("ROLE_JOB_SEEKER")
                .build();

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void expiredTokenIsRejected() {
        // Build a JwtService whose tokens expire immediately in the past,
        // to deterministically produce an expired token without sleeping
        // in the test.
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(TEST_SECRET);
        expiredProps.setExpirationMs(-1000L); // expiry is already in the past
        JwtService serviceWithExpiredTokens = new JwtService(expiredProps);

        String expiredToken = serviceWithExpiredTokens.generateToken("expired@example.com", Role.JOB_SEEKER);
        UserDetails userDetails = User.withUsername("expired@example.com")
                .password("irrelevant-hash")
                .authorities("ROLE_JOB_SEEKER")
                .build();

        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken("tampered@example.com", Role.JOB_SEEKER);
        // Flip the last character of the signature to invalidate it.
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        UserDetails userDetails = User.withUsername("tampered@example.com")
                .password("irrelevant-hash")
                .authorities("ROLE_JOB_SEEKER")
                .build();

        assertFalse(jwtService.isTokenValid(tampered, userDetails));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String token = jwtService.generateToken("wrongkey@example.com", Role.JOB_SEEKER);

        JwtProperties differentSecretProps = new JwtProperties();
        differentSecretProps.setSecret("a-completely-different-unit-test-secret-value-0987654321");
        differentSecretProps.setExpirationMs(60_000L);
        JwtService differentKeyService = new JwtService(differentSecretProps);

        UserDetails userDetails = User.withUsername("wrongkey@example.com")
                .password("irrelevant-hash")
                .authorities("ROLE_JOB_SEEKER")
                .build();

        assertFalse(differentKeyService.isTokenValid(token, userDetails));
    }
}
