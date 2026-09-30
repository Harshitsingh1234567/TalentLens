package com.resumeanalyzer.service;

import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Resolves the {@link User} entity that owns the current request, based on
 * the identity (email) carried by the validated JWT / Spring Security
 * {@link Authentication}.
 *
 * Used by services that must associate a newly created record (Resume,
 * JobDescription, and any future owned record) with the authenticated
 * caller, so that a user can never create a record under another user's
 * identity - the owner is always derived from the token, never from
 * client-supplied input.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User resolve(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user could not be reloaded - this indicates a data consistency issue"));
    }
}
