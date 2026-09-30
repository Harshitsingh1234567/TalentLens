package com.resumeanalyzer.exception;

/**
 * Thrown when a referenced record does not exist, or exists but is not
 * owned by the authenticated caller. Both cases are treated identically
 * (HTTP 404) and produce the same message, so a caller cannot use this
 * response to infer whether a given id exists under someone else's
 * account.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
