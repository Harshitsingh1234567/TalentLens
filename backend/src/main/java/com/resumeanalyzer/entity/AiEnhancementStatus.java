package com.resumeanalyzer.entity;

/**
 * Records what happened when {@link Analysis} was enriched with
 * Gemini-generated insights (Phase 5), so a caller can distinguish "the
 * model said there was nothing more to add" from "the AI layer was
 * unavailable" without needing separate booleans per section.
 *
 * This never affects the deterministic Phase 3/4 fields on {@link Analysis}
 * (matched/missing skills, scores, {@code atsIssues}, {@code formattingIssues},
 * {@code contentSuggestions}, {@code interviewQuestions}), which are always
 * populated exactly as before, regardless of this status.
 */
public enum AiEnhancementStatus {

    /** Gemini was not configured (disabled, or no API key present). */
    DISABLED,

    /** The call to Gemini failed (timeout, API error, or invalid/empty response). */
    FALLBACK,

    /** Gemini returned a usable, validated response. */
    SUCCESS
}
