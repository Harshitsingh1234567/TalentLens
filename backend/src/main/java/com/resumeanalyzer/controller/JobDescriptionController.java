package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.JobDescriptionRequest;
import com.resumeanalyzer.dto.JobDescriptionResponse;
import com.resumeanalyzer.dto.JobDescriptionSummaryResponse;
import com.resumeanalyzer.service.JobDescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Job Description ingestion API surface.
 *
 * Adds GET /api/job-descriptions (letting a Job Seeker discover/select
 * their own previously submitted job descriptions, e.g. for the matching
 * flow, without ever having to know or enter a numeric job description
 * id) alongside the original Phase 2 submission endpoint. Both endpoints
 * are protected by the existing default "authenticated()" security rule
 * from Phase 1 (no security configuration changes were needed for this).
 */
@RestController
@RequestMapping("/api/job-descriptions")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    public JobDescriptionController(JobDescriptionService jobDescriptionService) {
        this.jobDescriptionService = jobDescriptionService;
    }

    @PostMapping
    public ResponseEntity<JobDescriptionResponse> submit(@Valid @RequestBody JobDescriptionRequest request,
                                                           Authentication authentication) {
        JobDescriptionResponse response = jobDescriptionService.submit(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JobDescriptionSummaryResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(jobDescriptionService.listOwnJobDescriptions(authentication));
    }
}
