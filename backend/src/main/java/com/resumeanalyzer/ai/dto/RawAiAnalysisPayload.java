package com.resumeanalyzer.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The exact JSON shape requested from Gemini by
 * {@link com.resumeanalyzer.ai.prompt.AnalysisEnhancementPrompt}.
 *
 * This is intentionally a separate, "untrusted" type from the domain
 * result ({@code AiEnhancementResult}) - it is deserialized directly from
 * model output, so every field must be treated as unvalidated input until
 * {@code AiResponseSanitizer} has processed it. Never persisted or
 * returned from an API as-is.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawAiAnalysisPayload(
        List<String> advancedAtsIssues,
        List<String> formattingInsights,
        List<String> contentSuggestions,
        List<String> interviewQuestions) {
}
