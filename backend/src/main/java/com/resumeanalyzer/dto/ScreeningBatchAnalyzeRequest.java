package com.resumeanalyzer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for POST /api/screening-batches/{id}/analyze.
 *
 * The job description is supplied per analysis run, rather than fixed at
 * batch-creation time - exactly as anticipated by the Phase 6 design note
 * on {@code ScreeningBatch} ("Phase 7 may need that association shaped
 * differently, e.g. supplied per ranking run rather than fixed at
 * batch-creation time"). This also means the same batch can be
 * re-analyzed against a different job description later; each run simply
 * replaces the previous ranking for that batch (see
 * {@code ScreeningBatchRankingService}).
 *
 * The job description must belong to the same authenticated Recruiter as
 * the screening batch; ownership is enforced in
 * {@code ScreeningBatchRankingService}, not by trusting the request.
 */
public class ScreeningBatchAnalyzeRequest {

    @NotNull(message = "jobDescriptionId is required")
    private Long jobDescriptionId;

    public ScreeningBatchAnalyzeRequest() {
    }

    public ScreeningBatchAnalyzeRequest(Long jobDescriptionId) {
        this.jobDescriptionId = jobDescriptionId;
    }

    public Long getJobDescriptionId() {
        return jobDescriptionId;
    }

    public void setJobDescriptionId(Long jobDescriptionId) {
        this.jobDescriptionId = jobDescriptionId;
    }
}
