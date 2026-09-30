package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.AiEnhancementStatus;

import java.util.List;

/**
 * Response payload for GET /api/analysis/{id}/interview-questions.
 *
 * Serves the interview questions already persisted on the referenced
 * {@code Analysis} - both the deterministic, Phase 4 template-based set
 * ({@code questions}, unchanged) and, when available, the Phase 5
 * Gemini-generated, context-aware set ({@code aiQuestions}). No new
 * analysis or AI call happens here; this is a read-only view over data
 * computed once at {@code POST /api/analysis} time, consistent with the
 * rest of this API being free of hidden work on a GET.
 */
public class InterviewQuestionsResponse {

    private final Long analysisId;
    private final List<String> questions;
    private final List<String> aiQuestions;
    private final AiEnhancementStatus aiEnhancementStatus;

    public InterviewQuestionsResponse(Long analysisId,
                                       List<String> questions,
                                       List<String> aiQuestions,
                                       AiEnhancementStatus aiEnhancementStatus) {
        this.analysisId = analysisId;
        this.questions = questions;
        this.aiQuestions = aiQuestions;
        this.aiEnhancementStatus = aiEnhancementStatus;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public List<String> getAiQuestions() {
        return aiQuestions;
    }

    public AiEnhancementStatus getAiEnhancementStatus() {
        return aiEnhancementStatus;
    }
}
