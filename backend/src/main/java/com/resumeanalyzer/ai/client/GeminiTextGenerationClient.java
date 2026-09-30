package com.resumeanalyzer.ai.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import com.resumeanalyzer.ai.AiProviderException;
import com.resumeanalyzer.ai.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The ONLY class in this project that imports {@code com.google.genai.*}.
 * Every other class talks to the model through the {@link AiTextGenerationClient}
 * interface, so a future SDK change (or a different provider entirely) is
 * contained to this one file.
 *
 * Responsibilities kept deliberately narrow:
 *  - build a {@code Client} lazily, only once configuration is confirmed
 *    present (never eagerly, so a missing API key never breaks application
 *    startup);
 *  - enforce a hard wall-clock timeout independent of whatever the SDK
 *    does internally, so a slow/hanging call can never block the calling
 *    request indefinitely;
 *  - translate every possible failure (missing config, timeout, API
 *    error, empty response) into a sanitized {@link AiProviderException} -
 *    the raw SDK exception (which may embed request/response details) is
 *    logged, at most, by class name plus a short generic reason, never by
 *    its message or the prompt/response content;
 *  - (Phase 9) on a <em>transient</em> primary-model failure - HTTP
 *    429/503, a timeout, or a network-level failure - retry exactly once
 *    against {@link GeminiProperties#getFallbackModel()} before giving up.
 *    Non-transient failures (e.g. an empty/invalid response, or any other
 *    API error code) are not retried and propagate immediately, unchanged
 *    from the pre-Phase-9 behavior. Either way, the only thing callers
 *    ever see is a successful string or an {@link AiProviderException} -
 *    the model-fallback retry is entirely internal to this class and does
 *    not change the {@link AiTextGenerationClient} contract, the SUCCESS/
 *    FALLBACK/DISABLED status model in {@code GeminiAnalysisEnhancer}, or
 *    anything above this class;
 *  - (Phase 9) constrain both the primary and fallback model calls to the
 *    exact JSON shape {@code GeminiAnalysisEnhancer} deserializes into
 *    ({@code RESPONSE_SCHEMA} below) via the SDK's {@code responseSchema},
 *    in addition to {@code responseMimeType("application/json")}. This is
 *    a defense-in-depth measure against a model returning syntactically
 *    valid JSON in an unexpected shape (e.g. list items as objects instead
 *    of strings) - the same {@code RawAiAnalysisPayload}-driven parsing/
 *    fallback in {@code GeminiAnalysisEnhancer} still applies unchanged if
 *    a model disregards it.
 */
@Component
public class GeminiTextGenerationClient implements AiTextGenerationClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiTextGenerationClient.class);

    /**
     * The exact response shape {@code GeminiAnalysisEnhancer} deserializes
     * into (four fields, each an array of strings) - see {@code
     * RawAiAnalysisPayload}. Passed as {@code responseSchema} on every call
     * (both models) alongside {@code responseMimeType("application/json")}
     * so the API itself constrains the shape, rather than relying solely
     * on the prompt's free-text schema instructions. Built once; a {@code
     * Schema} is an immutable SDK value type, so it is safe to share
     * across every {@link #attemptGenerate(String, String)} call.
     */
    private static final Schema RESPONSE_SCHEMA = Schema.builder()
            .type(Type.Known.OBJECT)
            .properties(Map.of(
                    "advancedAtsIssues", stringArraySchema(),
                    "formattingInsights", stringArraySchema(),
                    "contentSuggestions", stringArraySchema(),
                    "interviewQuestions", stringArraySchema()))
            .build();

    private static Schema stringArraySchema() {
        return Schema.builder()
                .type(Type.Known.ARRAY)
                .items(Schema.builder().type(Type.Known.STRING).build())
                .build();
    }

    private final GeminiProperties properties;
    private final ExecutorService executor;

    private volatile Client client;

    public GeminiTextGenerationClient(GeminiProperties properties) {
        this.properties = properties;
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "gemini-client");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    @Override
    public String generate(String prompt) throws AiProviderException {
        if (!isAvailable()) {
            throw new AiProviderException(AiProviderException.Reason.NOT_CONFIGURED,
                    "Gemini is not configured (missing API key or disabled)");
        }

        return generateWithFallback(
                () -> attemptGenerate(prompt, properties.getModel()),
                () -> attemptGenerate(prompt, properties.getFallbackModel()));
    }

    /**
     * Runs {@code primary}; if it fails with a transient error (see
     * {@link #isTransient(AiProviderException)}), runs {@code fallback}
     * once and returns/throws its outcome instead. A non-transient primary
     * failure propagates immediately without ever invoking {@code fallback}.
     *
     * Package-private and expressed purely in terms of {@link GeminiAttempt}
     * (no Gemini SDK types) so the retry policy itself is directly unit
     * testable without contacting the real API - consistent with this
     * project's existing rule that no test may perform a live Gemini call.
     */
    String generateWithFallback(GeminiAttempt primary, GeminiAttempt fallback) throws AiProviderException {
        try {
            return primary.attempt();
        } catch (AiProviderException primaryFailure) {
            if (!isTransient(primaryFailure)) {
                throw primaryFailure;
            }
            log.warn("Gemini primary model {} failed transiently (reason={}); retrying once with fallback model {}",
                    properties.getModel(), primaryFailure.getReason(), properties.getFallbackModel());
            return fallback.attempt();
        }
    }

    /**
     * Whether {@code failure} came from a transient condition - HTTP
     * 429 (rate limit) or 503 (unavailable), a timeout, or a network-level
     * failure - for which retrying against a different model is a
     * reasonable, cheap thing to try. Any other API failure (e.g. 400/401/
     * 403) or an invalid/empty response is not transient: the model itself
     * is not the problem, so a different model would not help, and the
     * existing deterministic FALLBACK behavior applies immediately.
     */
    private boolean isTransient(AiProviderException failure) {
        if (failure.getReason() == AiProviderException.Reason.TIMEOUT) {
            return true;
        }
        if (failure.getReason() != AiProviderException.Reason.API_FAILURE) {
            return false;
        }
        Throwable cause = failure.getCause();
        if (cause instanceof com.google.genai.errors.ApiException apiException) {
            int code = apiException.code();
            return code == 429 || code == 503;
        }
        // A non-ApiException cause reaching here (e.g. connection reset,
        // DNS resolution failure) is a transport/network-level failure
        // rather than a structured API error - treated as transient per
        // the "...or network failure" clause of the fallback policy.
        return cause != null;
    }

    /**
     * A single attempt at producing a completion, already fully resolved
     * to a model. Thrown/returned outcomes are always the sanitized
     * {@link AiProviderException} contract - never a raw SDK exception.
     */
    @FunctionalInterface
    interface GeminiAttempt {
        String attempt() throws AiProviderException;
    }

    private String attemptGenerate(String prompt, String model) throws AiProviderException {
        Future<GenerateContentResponse> future = executor.submit(callGemini(prompt, model));
        try {
            GenerateContentResponse response = future.get(properties.getTimeoutSeconds(), TimeUnit.SECONDS);
            return extractText(response);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Gemini call to model {} timed out after {}s", model, properties.getTimeoutSeconds());
            throw new AiProviderException(AiProviderException.Reason.TIMEOUT,
                    "The AI provider did not respond in time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiProviderException(AiProviderException.Reason.API_FAILURE,
                    "The AI provider call was interrupted");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof com.google.genai.errors.ApiException apiException) {
                log.warn("Gemini call failed: {} code={} status={} model={}",
                        cause.getClass().getSimpleName(), apiException.code(), apiException.status(), model);
            } else {
                log.warn("Gemini call failed: {} model={}", cause.getClass().getSimpleName(), model);
            }
            throw new AiProviderException(AiProviderException.Reason.API_FAILURE,
                    "The AI provider failed to fulfil the request", cause);
        }
    }

    private Callable<GenerateContentResponse> callGemini(String prompt, String model) {
        return () -> {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .maxOutputTokens(properties.getMaxOutputTokens())
                    .temperature(0.4F)
                    .responseMimeType("application/json")
                    .responseSchema(RESPONSE_SCHEMA)
                    .build();

            return getOrCreateClient().models.generateContent(model, prompt, config);
        };
    }

    private String extractText(GenerateContentResponse response) throws AiProviderException {
        String text = response != null ? response.text() : null;
        if (text == null || text.isBlank()) {
            throw new AiProviderException(AiProviderException.Reason.INVALID_RESPONSE,
                    "The AI provider returned an empty response");
        }
        return text;
    }

    /**
     * Built lazily and cached, so the SDK client (and its HTTP resources)
     * is only ever constructed once - and never at all when Gemini is not
     * configured.
     */
    private Client getOrCreateClient() {
        Client existing = client;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (client == null) {
                client = Client.builder()
                        .apiKey(properties.getApiKey())
                        .httpOptions(HttpOptions.builder()
                                .timeout(properties.getTimeoutSeconds() * 1000)
                                .build())
                        .build();
            }
            return client;
        }
    }
}
