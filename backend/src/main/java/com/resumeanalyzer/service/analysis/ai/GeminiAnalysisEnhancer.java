package com.resumeanalyzer.service.analysis.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderException;
import com.resumeanalyzer.ai.AiResponseSanitizer;
import com.resumeanalyzer.ai.client.AiTextGenerationClient;
import com.resumeanalyzer.ai.dto.RawAiAnalysisPayload;
import com.resumeanalyzer.ai.prompt.AnalysisEnhancementPrompt;
import com.resumeanalyzer.entity.AiEnhancementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The single, Gemini-backed implementation of {@link AiAnalysisEnhancer}.
 *
 * Orchestration only: builds the prompt (via the centralized, versioned
 * {@link AnalysisEnhancementPrompt}), delegates the actual call to the
 * SDK-isolated {@link AiTextGenerationClient}, parses the response into
 * {@link RawAiAnalysisPayload}, and hands it to {@link AiResponseSanitizer}
 * before ever trusting it. Every failure mode - not configured, timeout,
 * API failure, malformed JSON, empty/unusable content - is caught here and
 * converted into a deterministic fallback; nothing above this class ever
 * needs to handle an AI-specific exception.
 *
 * Logging never includes resume/job-description content or raw provider
 * exception messages - only the prompt version, outcome, and (on failure)
 * a coarse {@link AiProviderException.Reason}.
 */
@Service
public class GeminiAnalysisEnhancer implements AiAnalysisEnhancer {

    private static final Logger log = LoggerFactory.getLogger(GeminiAnalysisEnhancer.class);

    private final AiTextGenerationClient aiTextGenerationClient;
    private final AiResponseSanitizer sanitizer;
    private final ObjectMapper objectMapper;

    public GeminiAnalysisEnhancer(AiTextGenerationClient aiTextGenerationClient,
                                   AiResponseSanitizer sanitizer,
                                   ObjectMapper objectMapper) {
        this.aiTextGenerationClient = aiTextGenerationClient;
        this.sanitizer = sanitizer;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiEnhancementResult enhance(AiEnhancementRequest request) {
        if (!aiTextGenerationClient.isAvailable()) {
            log.debug("AI enhancement skipped ({}): provider not configured", AnalysisEnhancementPrompt.VERSION);
            return AiEnhancementResult.disabled();
        }

        String prompt = AnalysisEnhancementPrompt.build(
                request.resumeText(), request.jobDescriptionText(),
                request.matchedSkills(), request.missingSkills());

        String rawText;
        try {
            rawText = aiTextGenerationClient.generate(prompt);
        } catch (AiProviderException e) {
            log.warn("AI enhancement failed ({}): {}", AnalysisEnhancementPrompt.VERSION, e.getReason());
            return AiEnhancementResult.fallback();
        }

        RawAiAnalysisPayload payload = parse(rawText);
        if (payload == null) {
            log.warn("AI enhancement failed ({}): {}", AnalysisEnhancementPrompt.VERSION,
                    AiProviderException.Reason.INVALID_RESPONSE);
            return AiEnhancementResult.fallback();
        }

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(payload);
        if (sanitized.isEffectivelyEmpty()) {
            log.warn("AI enhancement failed ({}): response contained no usable content",
                    AnalysisEnhancementPrompt.VERSION);
            return AiEnhancementResult.fallback();
        }

        log.debug("AI enhancement succeeded ({})", AnalysisEnhancementPrompt.VERSION);
        return new AiEnhancementResult(
                sanitized.advancedAtsIssues(),
                sanitized.formattingInsights(),
                sanitized.contentSuggestions(),
                sanitized.interviewQuestions(),
                AiEnhancementStatus.SUCCESS);
    }

    /**
     * Best-effort defensive parsing: models occasionally wrap JSON in
     * markdown code fences despite being told not to via {@code
     * responseMimeType=application/json} and explicit prompt instructions,
     * so a fenced response is still accepted rather than treated as
     * malformed. Any other parse failure returns {@code null}, signalling
     * an invalid response to the caller.
     */
    private RawAiAnalysisPayload parse(String rawText) {
        try {
            return objectMapper.readValue(stripCodeFences(rawText), RawAiAnalysisPayload.class);
        } catch (Exception e) {
            // Never log rawText itself: it is model output derived from the
            // resume/job-description content, so it must be treated with the
            // same care as the source data for logging purposes.
            log.warn("AI enhancement response could not be parsed as JSON ({})", e.getClass().getSimpleName());
            return null;
        }
    }

    private String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
