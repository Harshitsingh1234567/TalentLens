package com.resumeanalyzer.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for POST /api/matches.
 *
 * Both ids must reference records owned by the authenticated caller;
 * ownership is enforced in MatchService, not by trusting the request.
 */
public class MatchRequest {

    @NotNull(message = "resumeId is required")
    private Long resumeId;

    @NotNull(message = "jobDescriptionId is required")
    private Long jobDescriptionId;

    public MatchRequest() {
    }

    public MatchRequest(Long resumeId, Long jobDescriptionId) {
        this.resumeId = resumeId;
        this.jobDescriptionId = jobDescriptionId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public Long getJobDescriptionId() {
        return jobDescriptionId;
    }

    public void setJobDescriptionId(Long jobDescriptionId) {
        this.jobDescriptionId = jobDescriptionId;
    }
}
