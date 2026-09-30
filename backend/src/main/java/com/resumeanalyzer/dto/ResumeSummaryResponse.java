package com.resumeanalyzer.dto;

import java.time.Instant;

/**
 * One of the authenticated caller's own resumes, as returned by
 * GET /api/resumes - just enough to let a user pick a resume by name in
 * a selector UI. Deliberately excludes {@code extractedText}: a resume
 * list has no need for the full extracted text, which can be large, and
 * exposing it here would make this endpoint unnecessarily heavy (the
 * same reasoning already applied to {@code ScreeningBatchResumeSummary}
 * for the Phase 6 batch-overview list).
 */
public record ResumeSummaryResponse(Long id, String originalFilename, Instant uploadedAt) {
}
