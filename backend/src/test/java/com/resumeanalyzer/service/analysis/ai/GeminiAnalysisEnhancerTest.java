package com.resumeanalyzer.service.analysis.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderException;
import com.resumeanalyzer.ai.AiResponseSanitizer;
import com.resumeanalyzer.ai.client.AiTextGenerationClient;
import com.resumeanalyzer.entity.AiEnhancementStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Exercises every outcome {@link GeminiAnalysisEnhancer} must handle,
 * per the Phase 5 testing requirements: successful, malformed, empty and
 * failed AI responses, plus the "not configured" fallback path. No live
 * Gemini call is made anywhere in this test - {@link AiTextGenerationClient}
 * is fully mocked.
 */
@ExtendWith(MockitoExtension.class)
class GeminiAnalysisEnhancerTest {

    @Mock
    private AiTextGenerationClient aiTextGenerationClient;

    private GeminiAnalysisEnhancer enhancer;

    private final AiEnhancementRequest request = new AiEnhancementRequest(
            "Java developer with Spring Boot experience.",
            "Looking for a Java and AWS engineer.",
            Set.of("java"),
            Set.of("aws"));

    @BeforeEach
    void setUp() {
        enhancer = new GeminiAnalysisEnhancer(aiTextGenerationClient, new AiResponseSanitizer(), new ObjectMapper());
    }

    @Test
    void returnsDisabledResultWhenProviderIsNotConfigured() {
        when(aiTextGenerationClient.isAvailable()).thenReturn(false);

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.DISABLED, result.status());
        assertTrue(result.advancedAtsIssues().isEmpty());
        assertTrue(result.formattingInsights().isEmpty());
        assertTrue(result.contentSuggestions().isEmpty());
        assertTrue(result.interviewQuestions().isEmpty());
    }

    @Test
    void returnsSuccessResultForAWellFormedJsonResponse() throws AiProviderException {
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                {
                  "advancedAtsIssues": ["Keyword density for 'AWS' is low."],
                  "formattingInsights": ["Consider using consistent bullet punctuation."],
                  "contentSuggestions": ["Quantify the impact of the Spring Boot migration."],
                  "interviewQuestions": ["Walk me through your Spring Boot architecture decisions."]
                }
                """);

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.SUCCESS, result.status());
        assertEquals(List.of("Keyword density for 'AWS' is low."), result.advancedAtsIssues());
        assertEquals(List.of("Consider using consistent bullet punctuation."), result.formattingInsights());
        assertEquals(List.of("Quantify the impact of the Spring Boot migration."), result.contentSuggestions());
        assertEquals(List.of("Walk me through your Spring Boot architecture decisions."), result.interviewQuestions());
    }

    @Test
    void acceptsAJsonResponseWrappedInMarkdownCodeFences() throws AiProviderException {
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                "```json\n{\"advancedAtsIssues\":[\"issue\"],\"formattingInsights\":[],"
                        + "\"contentSuggestions\":[],\"interviewQuestions\":[]}\n```");

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.SUCCESS, result.status());
        assertEquals(List.of("issue"), result.advancedAtsIssues());
    }

    @Test
    void fallsBackOnMalformedJsonResponse() throws AiProviderException {
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("this is not valid json at all {{{");

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.FALLBACK, result.status());
        assertTrue(result.advancedAtsIssues().isEmpty());
    }

    @Test
    void fallsBackOnEmptyStructuredResponse() throws AiProviderException {
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn(
                "{\"advancedAtsIssues\":[],\"formattingInsights\":[],"
                        + "\"contentSuggestions\":[],\"interviewQuestions\":[]}");

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.FALLBACK, result.status());
    }

    @Test
    void fallsBackWhenProviderThrows() throws AiProviderException {
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new AiProviderException(AiProviderException.Reason.TIMEOUT, "timed out"));

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.FALLBACK, result.status());
    }

    @Test
    void fallsBackOnSyntacticallyValidJsonWithAWrongFieldElementType() throws AiProviderException {
        // Phase 9 parsing-safety regression test: the JSON is syntactically
        // valid (parses as JSON) but one field's array elements are objects
        // instead of the expected strings, which is exactly the shape this
        // codebase saw from a live Gemini fallback-model response. This must
        // be absorbed by GeminiAnalysisEnhancer#parse's catch-all (a Jackson
        // JsonMappingException/MismatchedInputException, not a JSON syntax
        // error) and turned into the same deterministic FALLBACK outcome as
        // any other unusable response - never propagate, never partially
        // apply the malformed field.
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn("""
                {
                  "advancedAtsIssues": [{"issue": "Keyword density for 'AWS' is low.", "severity": "high"}],
                  "formattingInsights": [],
                  "contentSuggestions": [],
                  "interviewQuestions": []
                }
                """);

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.FALLBACK, result.status());
        assertTrue(result.advancedAtsIssues().isEmpty());
        assertTrue(result.formattingInsights().isEmpty());
        assertTrue(result.contentSuggestions().isEmpty());
        assertTrue(result.interviewQuestions().isEmpty());
    }

    @Test
    void fallsBackWhenProviderReportsApiFailure() throws AiProviderException {
        when(aiTextGenerationClient.isAvailable()).thenReturn(true);
        when(aiTextGenerationClient.generate(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new AiProviderException(AiProviderException.Reason.API_FAILURE, "provider error"));

        AiEnhancementResult result = enhancer.enhance(request);

        assertEquals(AiEnhancementStatus.FALLBACK, result.status());
    }
}
