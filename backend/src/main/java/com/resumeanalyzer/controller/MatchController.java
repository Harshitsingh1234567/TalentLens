package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.MatchRequest;
import com.resumeanalyzer.dto.MatchResponse;
import com.resumeanalyzer.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Matching/scoring API surface.
 *
 * Per the locked Phase 3 API contract, this is limited to exactly one
 * endpoint. It is protected by the existing default "authenticated()"
 * security rule from Phase 1 (no security configuration changes were
 * needed for this) - both JOB_SEEKER and RECRUITER may call it, since
 * matching a resume against a job description is not role-specific
 * behavior; ownership enforcement (not role) is what scopes access, done
 * in {@link MatchService}.
 */
@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponse> match(@Valid @RequestBody MatchRequest request,
                                                Authentication authentication) {
        MatchResponse response = matchService.match(request, authentication);
        return ResponseEntity.ok(response);
    }
}
