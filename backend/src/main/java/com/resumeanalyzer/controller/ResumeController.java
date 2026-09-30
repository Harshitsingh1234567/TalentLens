package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.ResumeSummaryResponse;
import com.resumeanalyzer.dto.ResumeUploadResponse;
import com.resumeanalyzer.service.ResumeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Resume ingestion API surface.
 *
 * Adds GET /api/resumes (letting a Job Seeker discover/select their own
 * previously uploaded resumes, e.g. for the matching flow, without ever
 * having to know or enter a numeric resume id) alongside the original
 * Phase 2 upload endpoint. Both endpoints are protected by the existing
 * default "authenticated()" security rule from Phase 1 (no security
 * configuration changes were needed for this).
 *
 * Accepts multipart/form-data with a single "file" part and an optional
 * "candidateName" form field (used when a resume is uploaded on someone
 * else's behalf).
 */
@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(value = "candidateName", required = false) String candidateName,
                                                        Authentication authentication) {
        ResumeUploadResponse response = resumeService.upload(file, candidateName, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ResumeSummaryResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(resumeService.listOwnResumes(authentication));
    }
}
