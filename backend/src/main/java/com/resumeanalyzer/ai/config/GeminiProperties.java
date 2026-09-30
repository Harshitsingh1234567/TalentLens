package com.resumeanalyzer.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Phase 5 Gemini integration, bound to
 * the "ai.gemini" prefix in application.yml.
 *
 * Nothing sensitive is hard-coded: {@code apiKey} always comes from the
 * {@code GEMINI_API_KEY} environment variable (see application.yml) and
 * defaults to blank, never a real secret. Whether the AI layer is actually
 * used at runtime is gated by {@link #isConfigured()} - {@code enabled} is
 * a feature switch, while a blank {@code apiKey} independently disables
 * the feature regardless of that switch, so simply forgetting to unset
 * "enabled" in an environment without a key can never trigger a live call.
 *
 * This mirrors the existing {@code JwtProperties} /
 * {@code MatchingWeightsProperties} pattern already established in this
 * codebase (Phase 0/1): a plain, framework-only {@code @ConfigurationProperties}
 * holder with no business logic.
 */
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {

    /**
     * Feature switch for the Gemini-powered analysis enhancement. Defaults
     * to enabled; the actual gate on whether a call is ever attempted is
     * {@link #isConfigured()}, which also requires a non-blank API key.
     */
    private boolean enabled = true;

    /**
     * Gemini API key. MUST be supplied via the GEMINI_API_KEY environment
     * variable in any real environment - the default is intentionally
     * blank so the application never ships with a working key baked in.
     */
    private String apiKey = "";

    /**
     * Primary Gemini model id used for all Phase 5 analysis-enhancement
     * calls.
     */
    private String model = "gemini-3.6-flash";

    /**
     * Fallback Gemini model id. Used for a single retry attempt (Phase 9
     * reliability change) only when the primary model call fails with a
     * transient error (HTTP 429/503, timeout, or network failure) - see
     * {@link com.resumeanalyzer.ai.client.GeminiTextGenerationClient}. Not
     * consulted at all for non-transient failures (e.g. an invalid/empty
     * response), which fall straight through to the existing deterministic
     * FALLBACK behavior.
     */
    private String fallbackModel = "gemini-3.5-flash";

    /**
     * Per-call timeout. Kept short since this runs synchronously inside
     * the POST /api/analysis request and must always fall back cleanly
     * rather than hang the caller.
     */
    private int timeoutSeconds = 20;

    /**
     * Upper bound on generated output tokens, keeping cost/latency
     * predictable for the fixed-shape JSON this integration expects back.
     */
    private int maxOutputTokens = 2048;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * Whether the application is actually able to call Gemini right now:
     * the feature switch is on AND a non-blank API key is configured. Every
     * call site checks this (directly or transitively) before attempting a
     * network call, and falls back deterministically when it is false.
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
