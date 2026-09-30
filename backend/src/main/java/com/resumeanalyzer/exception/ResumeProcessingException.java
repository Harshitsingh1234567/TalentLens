package com.resumeanalyzer.exception;

/**
 * Thrown when an uploaded resume file cannot be accepted or parsed -
 * e.g. it is empty, has an unsupported extension, is corrupted, or
 * contains no extractable text. These are all treated as client input
 * problems (HTTP 400), since they stem from the uploaded file itself.
 */
public class ResumeProcessingException extends RuntimeException {

    public ResumeProcessingException(String message) {
        super(message);
    }

    public ResumeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
