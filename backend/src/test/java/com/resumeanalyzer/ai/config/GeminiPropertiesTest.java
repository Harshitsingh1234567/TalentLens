package com.resumeanalyzer.ai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiPropertiesTest {

    @Test
    void isConfiguredFalseByDefaultSinceApiKeyIsBlank() {
        GeminiProperties properties = new GeminiProperties();

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredFalseWhenDisabledEvenWithAnApiKey() {
        GeminiProperties properties = new GeminiProperties();
        properties.setApiKey("some-key");
        properties.setEnabled(false);

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredFalseWhenApiKeyIsBlankEvenIfEnabled() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEnabled(true);
        properties.setApiKey("   ");

        assertFalse(properties.isConfigured());
    }

    @Test
    void isConfiguredTrueWhenEnabledWithANonBlankApiKey() {
        GeminiProperties properties = new GeminiProperties();
        properties.setEnabled(true);
        properties.setApiKey("real-key");

        assertTrue(properties.isConfigured());
    }
}
