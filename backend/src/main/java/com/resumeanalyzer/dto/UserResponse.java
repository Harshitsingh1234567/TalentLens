package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.Role;

/**
 * Response payload for a successful POST /api/auth/register.
 *
 * Exposes only the identity fields a client needs after registering;
 * never the password or password hash. The {@link com.resumeanalyzer.entity.User}
 * JPA entity itself is never returned directly from the controller.
 */
public class UserResponse {

    private final Long id;
    private final String email;
    private final Role role;

    public UserResponse(Long id, String email, Role role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
