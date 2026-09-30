package com.resumeanalyzer.exception;

/**
 * Thrown when a registration request is made for an email address that is
 * already associated with an existing account.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
