package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.ScreeningBatchAnalyzeRequest;
import com.resumeanalyzer.dto.ScreeningBatchRankingResponse;
import com.resumeanalyzer.dto.ScreeningBatchResponse;
import com.resumeanalyzer.service.ScreeningBatchRankingService;
import com.resumeanalyzer.service.ScreeningBatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Recruiter screening-batch API surface: create a batch with a bulk
 * resume upload and retrieve one (Phase 6), plus analyzing a batch
 * against a job description and retrieving the resulting candidate
 * ranking (Phase 7). All four endpoints are protected by the existing
 * default "authenticated()" security rule from Phase 1 - no security
 * configuration change was needed for either phase. Role
 * (Recruiter-only) and ownership are enforced in the delegate services,
 * not here.
 */
@RestController
@RequestMapping("/api/screening-batches")
public class ScreeningBatchController {

    private final ScreeningBatchService screeningBatchService;
    private final ScreeningBatchRankingService screeningBatchRankingService;

    public ScreeningBatchController(ScreeningBatchService screeningBatchService,
                                     ScreeningBatchRankingService screeningBatchRankingService) {
        this.screeningBatchService = screeningBatchService;
        this.screeningBatchRankingService = screeningBatchRankingService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScreeningBatchResponse> create(@RequestParam("name") String name,
                                                          @RequestParam("files") List<MultipartFile> files,
                                                          Authentication authentication) {
        ScreeningBatchResponse response = screeningBatchService.createBatch(name, files, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScreeningBatchResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(screeningBatchService.getBatch(id, authentication));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ScreeningBatchRankingResponse> analyze(@PathVariable Long id,
                                                                  @Valid @RequestBody ScreeningBatchAnalyzeRequest request,
                                                                  Authentication authentication) {
        ScreeningBatchRankingResponse response = screeningBatchRankingService.analyze(id, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/ranking")
    public ResponseEntity<ScreeningBatchRankingResponse> getRanking(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(screeningBatchRankingService.getRanking(id, authentication));
    }
}
