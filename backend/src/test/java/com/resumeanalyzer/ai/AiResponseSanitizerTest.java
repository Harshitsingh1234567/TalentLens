package com.resumeanalyzer.ai;

import com.resumeanalyzer.ai.dto.RawAiAnalysisPayload;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiResponseSanitizerTest {

    private final AiResponseSanitizer sanitizer = new AiResponseSanitizer();

    @Test
    void handlesNullPayloadAsEmpty() {
        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(null);

        assertTrue(sanitized.isEffectivelyEmpty());
    }

    @Test
    void treatsNullListsAsEmptyRatherThanFailing() {
        RawAiAnalysisPayload raw = new RawAiAnalysisPayload(null, null, null, null);

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(raw);

        assertTrue(sanitized.isEffectivelyEmpty());
    }

    @Test
    void dropsBlankAndNullEntriesAndTrimsWhitespace() {
        RawAiAnalysisPayload raw = new RawAiAnalysisPayload(
                Arrays.asList("  Real issue  ", "", null, "   "),
                List.of(), List.of(), List.of());

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(raw);

        assertEquals(List.of("Real issue"), sanitized.advancedAtsIssues());
    }

    @Test
    void dedupesCaseInsensitivelyPreservingFirstOccurrence() {
        RawAiAnalysisPayload raw = new RawAiAnalysisPayload(
                List.of("Add more detail", "add MORE detail", "Something else"),
                List.of(), List.of(), List.of());

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(raw);

        assertEquals(List.of("Add more detail", "Something else"), sanitized.advancedAtsIssues());
    }

    @Test
    void capsListSizeAtTenItems() {
        List<String> fifteenItems = java.util.stream.IntStream.range(0, 15)
                .mapToObj(i -> "item " + i)
                .toList();
        RawAiAnalysisPayload raw = new RawAiAnalysisPayload(fifteenItems, List.of(), List.of(), List.of());

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(raw);

        assertEquals(10, sanitized.advancedAtsIssues().size());
    }

    @Test
    void capsItemLengthAt400Characters() {
        String longItem = "x".repeat(500);
        RawAiAnalysisPayload raw = new RawAiAnalysisPayload(List.of(longItem), List.of(), List.of(), List.of());

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(raw);

        assertEquals(400, sanitized.advancedAtsIssues().get(0).length());
    }

    @Test
    void isEffectivelyEmptyIsFalseWhenAnySingleSectionHasContent() {
        RawAiAnalysisPayload raw = new RawAiAnalysisPayload(List.of(), List.of(), List.of(), List.of("a question"));

        AiResponseSanitizer.Sanitized sanitized = sanitizer.sanitize(raw);

        assertTrue(!sanitized.isEffectivelyEmpty());
    }
}
