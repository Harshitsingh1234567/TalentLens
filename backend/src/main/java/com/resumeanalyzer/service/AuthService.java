package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AuthResponse;
import com.resumeanalyzer.dto.LoginRequest;
import com.resumeanalyzer.dto.RegisterRequest;
import com.resumeanalyzer.dto.UserResponse;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.DuplicateEmailException;
import com.resumeanalyzer.repository.UserRepository;
import com.resumeanalyzer.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the registration and login business logic. Controllers delegate
 * here rather than containing this logic themselves.
 *
 * Responsibilities:
 *   - normalizing and validating email uniqueness at registration time,
 *   - encoding passwords with the configured {@link PasswordEncoder} (BCrypt),
 *   - authenticating login attempts via Spring Security's
 *     {@link AuthenticationManager} (which delegates to
 *     {@link com.resumeanalyzer.security.CustomUserDetailsService} and the
 *     same PasswordEncoder),
 *   - issuing a signed JWT on successful login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("An account with this email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(normalizedEmail, encodedPassword, request.getRole());
        User saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        // Delegates credential verification to Spring Security's
        // AuthenticationManager (CustomUserDetailsService + PasswordEncoder).
        // On failure this throws an AuthenticationException (e.g.
        // BadCredentialsException) uniformly, whether the email is unknown
        // or the password is simply wrong - the caller never learns which.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user could not be reloaded - this indicates a data consistency issue"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new AuthResponse(token, "Bearer", user.getEmail(), user.getRole());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
