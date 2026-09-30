package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.Role;

/**
 * Response payload for a successful POST /api/auth/login.
 *
 * Intentionally contains only the issued token and the minimal identity
 * information the client needs - never the password or password hash.
 */
public class AuthResponse {

    private final String token;
    private final String tokenType;
    private final String email;
    private final Role role;

    public AuthResponse(String token, String tokenType, String email, Role role) {
        this.token = token;
        this.tokenType = tokenType;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
