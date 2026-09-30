package com.resumeanalyzer.ai;

/**
 * Thrown when the underlying AI provider (Gemini) cannot fulfil a request:
 * missing configuration, a timeout, an API-level failure, or an invalid/
 * empty response.
 *
 * The message is always a short, generic, pre-written classification -
 * never the raw provider exception message, stack trace, or any resume/
 * job-description content - so it is always safe to log or (in principle)
 * surface without leaking sensitive data or internal SDK details. Callers
 * in this codebase never let this exception reach the HTTP layer; it is
 * always caught by {@code GeminiAnalysisEnhancer} and translated into a
 * deterministic fallback result.
 */
public class AiProviderException extends Exception {

    public enum Reason {
        NOT_CONFIGURED,
        TIMEOUT,
        API_FAILURE,
        INVALID_RESPONSE
    }

    private final Reason reason;

    public AiProviderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AiProviderException(Reason reason, String message, Throwable cause) {
        // The cause is intentionally retained only for local logging at the
        // call site (see GeminiTextGenerationClient) via getCause()/class
        // name - never for inclusion in this exception's own message.
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
