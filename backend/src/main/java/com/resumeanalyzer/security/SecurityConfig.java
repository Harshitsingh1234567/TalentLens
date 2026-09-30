package com.resumeanalyzer.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase 1 Spring Security configuration.
 *
 * Replaces the Phase 0 permissive scaffold with real, stateless, JWT-based
 * authentication:
 *   - /api/auth/register and /api/auth/login remain publicly accessible,
 *   - every other endpoint requires a valid JWT (deny-by-default),
 *   - no HTTP sessions are used (SessionCreationPolicy.STATELESS),
 *   - CSRF protection is disabled, which is appropriate for a stateless,
 *     token-authenticated REST API that issues no browser cookies,
 *   - a missing or invalid JWT never results in an authenticated request;
 *     it simply falls through to the "authenticated()" rule below and is
 *     rejected with 401.
 *
 * There are still no protected business endpoints in Phase 1 - this
 * configuration only establishes the rule ("public auth endpoints, deny
 * everything else by default") that later phases' endpoints will rely on.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection is not applicable to a stateless REST API
            // authenticated via a bearer JWT rather than browser cookies.
            .csrf(csrf -> csrf.disable())
            // No server-side session is created or consulted; every
            // request must carry its own valid JWT.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .anyRequest().authenticated())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            // Return a plain JSON 401 instead of Spring Security's default
            // redirect/whitelabel behavior when authentication is missing
            // or invalid for a protected request.
            .exceptionHandling(handling -> handling
                    .authenticationEntryPoint((request, response, authException) ->
                            writeJsonError(response, 401, "Unauthorized", "A valid JWT is required to access this resource"))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            writeJsonError(response, 403, "Forbidden", "You do not have permission to access this resource")))
            // The JWT filter only populates the security context when a
            // valid token is present; it runs where the framework's own
            // username/password filter would otherwise run.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(jakarta.servlet.http.HttpServletResponse response,
                                 int status, String error, String message) throws java.io.IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
