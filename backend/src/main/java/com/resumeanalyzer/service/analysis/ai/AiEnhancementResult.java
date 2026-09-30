package com.resumeanalyzer.service.analysis.ai;

import com.resumeanalyzer.entity.AiEnhancementStatus;

import java.util.List;

/**
 * The outcome of {@link AiAnalysisEnhancer#enhance}. Always safe to
 * persist/return as-is: by the time this is constructed, every list has
 * already been through {@code AiResponseSanitizer} (for a {@link
 * AiEnhancementStatus#SUCCESS} result) or is simply empty (for {@link
 * AiEnhancementStatus#DISABLED}/{@link AiEnhancementStatus#FALLBACK}).
 *
 * Deliberately never throws and never null - callers (namely
 * {@code AnalysisService}) can treat every outcome, including provider
 * failure, uniformly as "here is the (possibly empty) enhancement to
 * apply", without their own try/catch.
 */
public record AiEnhancementResult(List<String> advancedAtsIssues,
                                   List<String> formattingInsights,
                                   List<String> contentSuggestions,
                                   List<String> interviewQuestions,
                                   AiEnhancementStatus status) {

    private static final AiEnhancementResult DISABLED =
            new AiEnhancementResult(List.of(), List.of(), List.of(), List.of(), AiEnhancementStatus.DISABLED);

    private static final AiEnhancementResult FALLBACK =
            new AiEnhancementResult(List.of(), List.of(), List.of(), List.of(), AiEnhancementStatus.FALLBACK);

    public static AiEnhancementResult disabled() {
        return DISABLED;
    }

    public static AiEnhancementResult fallback() {
        return FALLBACK;
    }
}
