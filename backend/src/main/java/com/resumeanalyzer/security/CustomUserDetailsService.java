package com.resumeanalyzer.security;

import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Integrates the existing {@link User} entity with Spring Security.
 *
 * The user's email is the authentication identity (there is no separate
 * username field), and the user's {@link com.resumeanalyzer.entity.Role}
 * is mapped to a single Spring Security authority using the
 * "ROLE_&lt;ROLE_NAME&gt;" convention (e.g. ROLE_JOB_SEEKER, ROLE_RECRUITER),
 * consistently with the same convention used when reading the role claim
 * out of a JWT in {@link JwtAuthenticationFilter}.
 *
 * No separate authentication-only user representation is introduced; the
 * existing User entity remains the single persisted user record.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for the given email"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
