package com.resumeanalyzer.repository;

import com.resumeanalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for User entities.
 *
 * Phase 0 scope: only the lookup method genuinely required by the planned
 * Phase 1 authentication workflow (login-by-email, register-time duplicate
 * check) is defined here. No speculative query methods are added.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
