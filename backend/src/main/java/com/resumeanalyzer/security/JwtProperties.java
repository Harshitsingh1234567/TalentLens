package com.resumeanalyzer.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized JWT configuration, bound to the "security.jwt" prefix in
 * application.yml.
 *
 * The secret and expiration are intentionally configuration-driven rather
 * than Java constants, so they can be overridden per-environment (e.g. via
 * the JWT_SECRET and JWT_EXPIRATION_MS environment variables) without a
 * code change.
 *
 * The default secret below is a development-only fallback. It is long
 * enough to satisfy the HMAC-SHA256 minimum key length, but it is NOT a
 * real secret and MUST be overridden via the JWT_SECRET environment
 * variable in any shared or production environment.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret = "dev-only-insecure-default-secret-do-not-use-in-production-change-me";

    private long expirationMs = 3_600_000L; // 1 hour

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}
