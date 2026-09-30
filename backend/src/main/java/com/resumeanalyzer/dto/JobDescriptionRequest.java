package com.resumeanalyzer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for POST /api/job-descriptions.
 *
 * Size limit decision: the specification does not define a maximum JD
 * length. A generous 20,000-character cap is enforced as a reasonable
 * defensive bound against pathological/abusive payloads, comfortably
 * above the length of any realistic job posting.
 */
public class JobDescriptionRequest {

    @NotBlank(message = "Job description content is required")
    @Size(max = 20_000, message = "Job description content must not exceed 20000 characters")
    private String content;

    public JobDescriptionRequest() {
    }

    public JobDescriptionRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
