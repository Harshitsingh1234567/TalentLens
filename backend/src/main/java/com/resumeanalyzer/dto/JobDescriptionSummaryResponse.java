package com.resumeanalyzer.dto;

import java.time.Instant;

/**
 * One of the authenticated caller's own job descriptions, as returned by
 * GET /api/job-descriptions - just enough to let a user pick a job
 * description in a selector UI. {@code JobDescription} has no title
 * field (Phase 2 scope is plain pasted text only), so {@code
 * contentPreview} is a short, server-truncated excerpt of the existing
 * {@code content} field (see {@code JobDescriptionService#buildPreview})
 * rather than the full, potentially large, submitted text.
 */
public record JobDescriptionSummaryResponse(Long id, String contentPreview, Instant submittedAt) {
}
