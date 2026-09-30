package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.ResumeFileType;

import java.time.Instant;

/**
 * One resume ("candidate") already persisted as part of a screening
 * batch. Deliberately excludes {@code extractedText}: the full extracted
 * text of every resume in a potentially large batch would make this
 * response unnecessarily heavy for what is meant to be an overview list,
 * and Phase 6 has no per-resume detail endpoint to make use of it. Phase
 * 7 (ranking/matching) reads {@code Resume.extractedText} directly from
 * the repository server-side; it does not need it round-tripped through
 * this DTO.
 */
public record ScreeningBatchResumeSummary(Long resumeId,
                                           String originalFilename,
                                           ResumeFileType fileType,
                                           String candidateName,
                                           Instant uploadedAt) {
}
