package com.resumeanalyzer.ai.client;

import com.resumeanalyzer.ai.AiProviderException;
import com.resumeanalyzer.ai.config.GeminiProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers only the configuration-gated behavior of {@link GeminiTextGenerationClient}
 * that does not require an actual SDK/network call - the "missing
 * configuration" path is exactly the one this test suite must exercise
 * without ever contacting the real Gemini API. The happy-path SDK call
 * itself is exercised indirectly via {@code GeminiAnalysisEnhancerTest},
 * which mocks this class's {@link AiTextGenerationClient} interface
 * instead of touching the SDK.
 *
 * The Phase 9 primary/fallback-model retry policy ({@code generateWithFallback})
 * is also covered here, purely at the {@link GeminiTextGenerationClient.GeminiAttempt}
 * level (plain lambdas returning/throwing) - this exercises the retry
 * decision exhaustively without ever constructing a real Gemini SDK type,
 * consistent with the "no live Gemini call in any test" rule above. What
 * happens once {@code generate} finally throws (i.e. that it turns into
 * {@code AiEnhancementStatus.FALLBACK}) is already covered, unmodified, by
 * {@code GeminiAnalysisEnhancerTest#fallsBackWhenProviderThrows} and
 * {@code #fallsBackWhenProviderReportsApiFailure}.
 */
class GeminiTextGenerationClientTest {

    @Test
    void isAvailableReflectsGeminiPropertiesConfiguration() {
        GeminiProperties properties = new GeminiProperties();
        GeminiTextGenerationClient client = new GeminiTextGenerationClient(properties);

        assertFalse(client.isAvailable());

        properties.setEnabled(true);
        properties.setApiKey("a-key");

        assertEquals(properties.isConfigured(), client.isAvailable());
    }

    @Test
    void generateThrowsNotConfiguredWithoutEverCallingTheSdkWhenApiKeyIsMissing() {
        GeminiProperties properties = new GeminiProperties();
        GeminiTextGenerationClient client = new GeminiTextGenerationClient(properties);

        AiProviderException exception = assertThrows(AiProviderException.class, () -> client.generate("prompt"));

        assertEquals(AiProviderException.Reason.NOT_CONFIGURED, exception.getReason());
    }

    @Test
    void generateWithFallbackReturnsThePrimaryResultWithoutEverInvokingTheFallbackModel() throws AiProviderException {
        GeminiTextGenerationClient client = new GeminiTextGenerationClient(new GeminiProperties());
        AtomicBoolean fallbackInvoked = new AtomicBoolean(false);

        String result = client.generateWithFallback(
                () -> "primary model result",
                () -> {
                    fallbackInvoked.set(true);
                    return "fallback model result";
                });

        assertEquals("primary model result", result);
        assertFalse(fallbackInvoked.get(), "fallback model must not be called when the primary model succeeds");
    }

    @Test
    void generateWithFallbackRetriesTheFallbackModelOnceAfterATransientPrimaryFailure() throws AiProviderException {
        GeminiTextGenerationClient client = new GeminiTextGenerationClient(new GeminiProperties());

        String result = client.generateWithFallback(
                () -> {
                    throw new AiProviderException(AiProviderException.Reason.TIMEOUT, "primary model timed out");
                },
                () -> "fallback model result");

        assertEquals("fallback model result", result);
    }

    @Test
    void generateWithFallbackPropagatesTheFallbackFailureWhenBothModelsFail() {
        GeminiTextGenerationClient client = new GeminiTextGenerationClient(new GeminiProperties());

        AiProviderException exception = assertThrows(AiProviderException.class, () -> client.generateWithFallback(
                () -> {
                    throw new AiProviderException(AiProviderException.Reason.TIMEOUT, "primary model timed out");
                },
                () -> {
                    throw new AiProviderException(AiProviderException.Reason.API_FAILURE,
                            "fallback model failed", new IOException("connection reset"));
                }));

        assertEquals(AiProviderException.Reason.API_FAILURE, exception.getReason());
    }

    @Test
    void generateWithFallbackDoesNotRetryANonTransientPrimaryFailure() {
        GeminiTextGenerationClient client = new GeminiTextGenerationClient(new GeminiProperties());
        AtomicBoolean fallbackInvoked = new AtomicBoolean(false);

        AiProviderException exception = assertThrows(AiProviderException.class, () -> client.generateWithFallback(
                () -> {
                    throw new AiProviderException(AiProviderException.Reason.INVALID_RESPONSE, "empty response");
                },
                () -> {
                    fallbackInvoked.set(true);
                    return "fallback model result";
                }));

        assertEquals(AiProviderException.Reason.INVALID_RESPONSE, exception.getReason());
        assertFalse(fallbackInvoked.get(), "a non-transient failure (e.g. an invalid response) must not retry");
    }
}
