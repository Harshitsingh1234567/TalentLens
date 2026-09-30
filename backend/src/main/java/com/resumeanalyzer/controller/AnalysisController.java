package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.AnalysisRequest;
import com.resumeanalyzer.dto.AnalysisResponse;
import com.resumeanalyzer.dto.InterviewQuestionsResponse;
import com.resumeanalyzer.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Job Seeker single-analysis API surface.
 *
 * The locked Phase 4 API contract (POST /api/analysis, GET
 * /api/analysis/{id}) is unchanged. Phase 5 adds one read-only endpoint,
 * GET /api/analysis/{id}/interview-questions, exposing the (deterministic
 * and, when available, AI-generated) interview questions already
 * computed and stored at analysis-creation time. All three endpoints are
 * protected by the existing default "authenticated()" security rule from
 * Phase 1 - no security configuration changes were needed. Ownership (not
 * role) scopes access, enforced in {@link AnalysisService}.
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> create(@Valid @RequestBody AnalysisRequest request,
                                                    Authentication authentication) {
        AnalysisResponse response = analysisService.createAnalysis(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalysisResponse> getById(@PathVariable Long id, Authentication authentication) {
        AnalysisResponse response = analysisService.getAnalysis(id, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/interview-questions")
    public ResponseEntity<InterviewQuestionsResponse> getInterviewQuestions(@PathVariable Long id,
                                                                             Authentication authentication) {
        InterviewQuestionsResponse response = analysisService.getInterviewQuestions(id, authentication);
        return ResponseEntity.ok(response);
    }
}
