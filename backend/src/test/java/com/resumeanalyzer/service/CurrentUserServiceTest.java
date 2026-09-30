package com.resumeanalyzer.service;

import com.resumeanalyzer.entity.Role;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserService(userRepository);
    }

    @Test
    void resolvesUserByAuthenticationName() {
        User user = new User("jobseeker@example.com", "$2a$10$hash", Role.JOB_SEEKER);
        when(authentication.getName()).thenReturn("jobseeker@example.com");
        when(userRepository.findByEmail("jobseeker@example.com")).thenReturn(Optional.of(user));

        User resolved = currentUserService.resolve(authentication);

        assertEquals(user, resolved);
    }

    @Test
    void throwsIllegalStateWhenAuthenticatedUserNoLongerExists() {
        when(authentication.getName()).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> currentUserService.resolve(authentication));
    }
}
