package com.resumeanalyzer.ai;

import com.resumeanalyzer.ai.dto.RawAiAnalysisPayload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates and sanitizes the raw, untrusted JSON payload returned by
 * Gemini before any of it is persisted or returned from the API.
 *
 * Rules enforced, per list:
 *  - null list -> treated as empty (never a validation failure by itself -
 *    a genuinely empty section, e.g. no formatting issues found, is valid)
 *  - null/blank entries dropped
 *  - each entry trimmed and capped to {@link #MAX_ITEM_LENGTH} characters
 *  - duplicate entries (case-insensitive) removed, order preserved
 *  - list capped to {@link #MAX_ITEMS_PER_LIST} entries
 *
 * A payload is rejected outright (treated as an invalid/empty response by
 * the caller) only if every one of the four sections ends up empty after
 * sanitization - i.e. the model returned nothing usable at all.
 */
@Component
public class AiResponseSanitizer {

    private static final int MAX_ITEM_LENGTH = 400;
    private static final int MAX_ITEMS_PER_LIST = 10;

    public record Sanitized(List<String> advancedAtsIssues,
                             List<String> formattingInsights,
                             List<String> contentSuggestions,
                             List<String> interviewQuestions) {

        public boolean isEffectivelyEmpty() {
            return advancedAtsIssues.isEmpty() && formattingInsights.isEmpty()
                    && contentSuggestions.isEmpty() && interviewQuestions.isEmpty();
        }
    }

    public Sanitized sanitize(RawAiAnalysisPayload raw) {
        if (raw == null) {
            return new Sanitized(List.of(), List.of(), List.of(), List.of());
        }
        return new Sanitized(
                sanitizeList(raw.advancedAtsIssues()),
                sanitizeList(raw.formattingInsights()),
                sanitizeList(raw.contentSuggestions()),
                sanitizeList(raw.interviewQuestions()));
    }

    private List<String> sanitizeList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();

        for (String item : items) {
            if (item == null) {
                continue;
            }
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_ITEM_LENGTH) {
                trimmed = trimmed.substring(0, MAX_ITEM_LENGTH).trim();
            }
            String dedupeKey = trimmed.toLowerCase();
            if (!seen.add(dedupeKey)) {
                continue;
            }
            result.add(trimmed);
            if (result.size() >= MAX_ITEMS_PER_LIST) {
                break;
            }
        }

        return List.copyOf(result);
    }
}
