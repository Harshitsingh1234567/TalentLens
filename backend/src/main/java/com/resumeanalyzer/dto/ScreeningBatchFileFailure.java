package com.resumeanalyzer.dto;

/**
 * One file from a bulk-upload request that could not be turned into a
 * Resume - e.g. empty, unsupported extension, corrupted, or no
 * extractable text. Reported transparently rather than silently dropped
 * (see {@code ScreeningBatchService#createBatch}); {@code reason} is
 * exactly the message from the {@code ResumeProcessingException} raised
 * for that file, reusing the Phase 2 validation wording as-is.
 */
public record ScreeningBatchFileFailure(String originalFilename, String reason) {
}
