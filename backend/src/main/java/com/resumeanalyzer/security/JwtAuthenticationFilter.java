package com.resumeanalyzer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates incoming requests that carry a valid "Authorization: Bearer
 * &lt;token&gt;" header.
 *
 * This filter never rejects a request itself (it does not send an error
 * response); it only populates the {@link SecurityContextHolder} when a
 * valid token is present. Whether the request is ultimately allowed is
 * decided by the authorization rules configured in
 * {@link SecurityConfig} - a missing or invalid token simply leaves the
 * request unauthenticated, which those rules will then reject for any
 * endpoint other than the public authentication endpoints.
 *
 * The filter never logs the token value, the JWT secret, or any decoded
 * credential.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Only attempt to authenticate if no authentication is already
        // present for this request (keeps the filter idempotent/stateless).
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateIfValid(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateIfValid(String token, HttpServletRequest request) {
        try {
            String email = jwtService.extractEmail(token);
            if (email == null) {
                return;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (RuntimeException ex) {
            // Any parsing failure, unknown subject, or validation failure
            // simply results in "not authenticated" - no error is exposed
            // here, and nothing sensitive is logged.
            SecurityContextHolder.clearContext();
        }
    }
}
