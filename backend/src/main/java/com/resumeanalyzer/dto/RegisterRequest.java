package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /api/auth/register.
 *
 * Password policy decision: the specification does not define an exact
 * password-strength policy, so a reasonable minimal secure baseline is
 * enforced here - a minimum of 8 characters. No additional complexity
 * rules (mandatory symbols/uppercase/digits, etc.) are imposed, to avoid
 * inventing requirements beyond what the project actually needs.
 */
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a well-formed address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters long")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    public RegisterRequest() {
    }

    public RegisterRequest(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
