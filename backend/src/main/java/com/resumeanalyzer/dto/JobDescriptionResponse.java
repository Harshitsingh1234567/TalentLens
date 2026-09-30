package com.resumeanalyzer.dto;

import java.time.Instant;

/**
 * Response payload for a successful POST /api/job-descriptions.
 *
 * Contains only the persisted, owner-scoped record - never any other
 * user's data.
 */
public class JobDescriptionResponse {

    private final Long id;
    private final String content;
    private final Instant submittedAt;

    public JobDescriptionResponse(Long id, String content, Instant submittedAt) {
        this.id = id;
        this.content = content;
        this.submittedAt = submittedAt;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
