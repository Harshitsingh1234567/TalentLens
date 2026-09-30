package com.resumeanalyzer.service;

import com.resumeanalyzer.dto.AuthResponse;
import com.resumeanalyzer.dto.LoginRequest;
import com.resumeanalyzer.dto.RegisterRequest;
import com.resumeanalyzer.dto.UserResponse;
import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.DuplicateEmailException;
import com.resumeanalyzer.repository.UserRepository;
import com.resumeanalyzer.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void registerSucceedsForNewEmailAndStoresEncodedPassword() {
        RegisterRequest request = new RegisterRequest("New.User@Example.com", "plaintext-password", Role.JOB_SEEKER);

        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("$2a$10$encodedHashValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(1L);
            return toSave;
        });

        UserResponse response = authService.register(request);

        assertEquals(1L, response.getId());
        assertEquals("new.user@example.com", response.getEmail());
        assertEquals(Role.JOB_SEEKER, response.getRole());

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        // Email is normalized (trimmed/lower-cased) before persistence.
        assertEquals("new.user@example.com", savedUser.getEmail());
        // The stored password must never be the plaintext value.
        assertNotEquals("plaintext-password", savedUser.getPassword());
        assertEquals("$2a$10$encodedHashValue", savedUser.getPassword());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "plaintext-password", Role.RECRUITER);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSucceedsAndReturnsSignedToken() {
        LoginRequest request = new LoginRequest("jobseeker@example.com", "correct-password");
        User storedUser = new User("jobseeker@example.com", "$2a$10$storedHash", Role.JOB_SEEKER);
        storedUser.setId(5L);

        when(userRepository.findByEmail("jobseeker@example.com")).thenReturn(Optional.of(storedUser));
        when(jwtService.generateToken("jobseeker@example.com", Role.JOB_SEEKER)).thenReturn("signed.jwt.token");

        AuthResponse response = authService.login(request);

        assertEquals("signed.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("jobseeker@example.com", response.getEmail());
        assertEquals(Role.JOB_SEEKER, response.getRole());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        UsernamePasswordAuthenticationToken submittedAuth = authCaptor.getValue();
        assertEquals("jobseeker@example.com", submittedAuth.getPrincipal());
        assertEquals("correct-password", submittedAuth.getCredentials());
    }

    @Test
    void loginPropagatesGenericAuthenticationFailureForBadCredentials() {
        LoginRequest request = new LoginRequest("jobseeker@example.com", "wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(jwtService, never()).generateToken(anyString(), any(Role.class));
    }
}
