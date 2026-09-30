package com.resumeanalyzer.dto;

import com.resumeanalyzer.entity.AiEnhancementStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * The consolidated response for both POST /api/analysis and
 * GET /api/analysis/{id} - match score breakdown, matched/missing
 * skills, ATS issues, formatting issues, content suggestions, and
 * interview questions, all in one payload.
 *
 * Phase 5 adds the {@code ai*} fields and {@code aiEnhancementStatus}
 * alongside the original (Phase 3/4) fields above them, which keep their
 * exact original meaning and are always populated the same way regardless
 * of whether the AI enhancement succeeded, was skipped, or fell back.
 */
public class AnalysisResponse {

    private final Long id;
    private final Long resumeId;
    private final Long jobDescriptionId;
    private final Set<String> matchedSkills;
    private final Set<String> missingSkills;
    private final double skillScore;
    private final double experienceScore;
    private final double semanticScore;
    private final double finalScore;
    private final String scoreExplanation;
    private final List<String> atsIssues;
    private final List<String> formattingIssues;
    private final List<String> contentSuggestions;
    private final List<String> interviewQuestions;
    private final Instant createdAt;
    private final List<String> aiAdvancedAtsIssues;
    private final List<String> aiFormattingInsights;
    private final List<String> aiContentSuggestions;
    private final List<String> aiInterviewQuestions;
    private final AiEnhancementStatus aiEnhancementStatus;

    public AnalysisResponse(Long id,
                             Long resumeId,
                             Long jobDescriptionId,
                             Set<String> matchedSkills,
                             Set<String> missingSkills,
                             double skillScore,
                             double experienceScore,
                             double semanticScore,
                             double finalScore,
                             String scoreExplanation,
                             List<String> atsIssues,
                             List<String> formattingIssues,
                             List<String> contentSuggestions,
                             List<String> interviewQuestions,
                             Instant createdAt,
                             List<String> aiAdvancedAtsIssues,
                             List<String> aiFormattingInsights,
                             List<String> aiContentSuggestions,
                             List<String> aiInterviewQuestions,
                             AiEnhancementStatus aiEnhancementStatus) {
        this.id = id;
        this.resumeId = resumeId;
        this.jobDescriptionId = jobDescriptionId;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.skillScore = skillScore;
        this.experienceScore = experienceScore;
        this.semanticScore = semanticScore;
        this.finalScore = finalScore;
        this.scoreExplanation = scoreExplanation;
        this.atsIssues = atsIssues;
        this.formattingIssues = formattingIssues;
        this.contentSuggestions = contentSuggestions;
        this.interviewQuestions = interviewQuestions;
        this.createdAt = createdAt;
        this.aiAdvancedAtsIssues = aiAdvancedAtsIssues;
        this.aiFormattingInsights = aiFormattingInsights;
        this.aiContentSuggestions = aiContentSuggestions;
        this.aiInterviewQuestions = aiInterviewQuestions;
        this.aiEnhancementStatus = aiEnhancementStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public Long getJobDescriptionId() {
        return jobDescriptionId;
    }

    public Set<String> getMatchedSkills() {
        return matchedSkills;
    }

    public Set<String> getMissingSkills() {
        return missingSkills;
    }

    public double getSkillScore() {
        return skillScore;
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public double getSemanticScore() {
        return semanticScore;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public String getScoreExplanation() {
        return scoreExplanation;
    }

    public List<String> getAtsIssues() {
        return atsIssues;
    }

    public List<String> getFormattingIssues() {
        return formattingIssues;
    }

    public List<String> getContentSuggestions() {
        return contentSuggestions;
    }

    public List<String> getInterviewQuestions() {
        return interviewQuestions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<String> getAiAdvancedAtsIssues() {
        return aiAdvancedAtsIssues;
    }

    public List<String> getAiFormattingInsights() {
        return aiFormattingInsights;
    }

    public List<String> getAiContentSuggestions() {
        return aiContentSuggestions;
    }

    public List<String> getAiInterviewQuestions() {
        return aiInterviewQuestions;
    }

    public AiEnhancementStatus getAiEnhancementStatus() {
        return aiEnhancementStatus;
    }
}
