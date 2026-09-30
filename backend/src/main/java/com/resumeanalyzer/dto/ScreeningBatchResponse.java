package com.resumeanalyzer.dto;

import java.time.Instant;
import java.util.List;

/**
 * The response for both POST /api/screening-batches (create + bulk
 * upload) and GET /api/screening-batches/{id} (retrieve).
 *
 * {@code failedFiles} is always empty on a GET - a failed file is never
 * persisted, so there is nothing left to report once the batch already
 * exists; it is only ever non-empty in the response to the POST that
 * encountered the failure. Kept as a single DTO shape (rather than one
 * type per endpoint) since the two responses otherwise describe exactly
 * the same resource, matching this project's existing convention (see
 * {@code AnalysisResponse}, shared by POST and GET /api/analysis).
 */
public class ScreeningBatchResponse {

    private final Long id;
    private final String name;
    private final Instant createdAt;
    private final List<ScreeningBatchResumeSummary> resumes;
    private final List<ScreeningBatchFileFailure> failedFiles;

    public ScreeningBatchResponse(Long id,
                                   String name,
                                   Instant createdAt,
                                   List<ScreeningBatchResumeSummary> resumes,
                                   List<ScreeningBatchFileFailure> failedFiles) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.resumes = resumes;
        this.failedFiles = failedFiles;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ScreeningBatchResumeSummary> getResumes() {
        return resumes;
    }

    public List<ScreeningBatchFileFailure> getFailedFiles() {
        return failedFiles;
    }
}
