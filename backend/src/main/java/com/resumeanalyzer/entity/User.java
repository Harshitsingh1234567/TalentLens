package com.resumeanalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * User entity representing an authenticated account.
 *
 * Phase 0 scope: this class only defines the persistent shape of a user.
 * No password hashing logic, no authentication logic, and no JWT handling
 * are implemented here - those belong to Phase 1.
 *
 * The account holder is either a JOB_SEEKER or a RECRUITER, as chosen at
 * registration time (registration itself is implemented in Phase 1).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The account's login identity. Used as the unique credential to
     * authenticate against in Phase 1 (e.g. /api/auth/login).
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Stores a password hash. Phase 0 does not implement the hashing
     * logic itself - that is introduced together with the authentication
     * workflow in Phase 1.
     */
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    public User() {
    }

    public User(String email, String password, Role role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
