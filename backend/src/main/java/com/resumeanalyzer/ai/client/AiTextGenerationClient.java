package com.resumeanalyzer.ai.client;

import com.resumeanalyzer.ai.AiProviderException;

/**
 * A minimal, provider-agnostic seam for "send a text prompt, get a text
 * completion back". This is the abstraction the rest of the application
 * (see {@code com.resumeanalyzer.service.analysis.ai}) depends on -
 * nothing outside the {@code com.resumeanalyzer.ai.client} package needs
 * to know that Gemini, specifically, is behind it.
 *
 * The only implementation today is {@link GeminiTextGenerationClient}. A
 * future alternative provider (or a test double) is a matter of adding
 * another implementation of this interface; no other code would need to
 * change.
 */
public interface AiTextGenerationClient {

    /**
     * Whether this client is configured and able to accept calls right
     * now. Callers must check this first and fall back deterministically
     * when it is {@code false}, rather than calling {@link #generate} and
     * relying on the resulting exception.
     */
    boolean isAvailable();

    /**
     * Sends {@code prompt} to the underlying model and returns its raw
     * text completion.
     *
     * @throws AiProviderException if the provider is not configured, the
     *                              call times out, the provider reports a
     *                              failure, or the response is empty/
     *                              structurally invalid. The exception
     *                              message is always sanitized - see
     *                              {@link AiProviderException}.
     */
    String generate(String prompt) throws AiProviderException;
}
