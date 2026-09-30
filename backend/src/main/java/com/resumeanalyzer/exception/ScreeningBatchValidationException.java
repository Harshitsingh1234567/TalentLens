package com.resumeanalyzer.exception;

/**
 * Thrown when a screening-batch create request is invalid at the
 * request level - a blank/oversized batch name, zero resume files
 * provided, or too many files in a single request. These are always
 * client input problems (HTTP 400).
 *
 * Deliberately distinct from {@link ResumeProcessingException}: that
 * exception is about one uploaded file being unusable (and is handled
 * per-file, without failing the whole batch - see
 * {@code ScreeningBatchService}), whereas this one is about the request
 * as a whole being malformed, before any file is even looked at.
 */
public class ScreeningBatchValidationException extends RuntimeException {

    public ScreeningBatchValidationException(String message) {
        super(message);
    }
}
