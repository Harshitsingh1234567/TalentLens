package com.resumeanalyzer.dto;

import java.util.Set;

/**
 * Response payload for a successful POST /api/matches.
 *
 * Exposes every score component individually (not just the final score)
 * so the result is transparent and explainable, per the Phase 3
 * requirement that component scores be available, not just the blended
 * total.
 */
public class MatchResponse {

    private final Long resumeId;
    private final Long jobDescriptionId;
    private final Set<String> matchedSkills;
    private final Set<String> missingSkills;
    private final double skillScore;
    private final double experienceScore;
    private final double semanticScore;
    private final double finalScore;
    private final String explanation;

    public MatchResponse(Long resumeId,
                          Long jobDescriptionId,
                          Set<String> matchedSkills,
                          Set<String> missingSkills,
                          double skillScore,
                          double experienceScore,
                          double semanticScore,
                          double finalScore,
                          String explanation) {
        this.resumeId = resumeId;
        this.jobDescriptionId = jobDescriptionId;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.skillScore = skillScore;
        this.experienceScore = experienceScore;
        this.semanticScore = semanticScore;
        this.finalScore = finalScore;
        this.explanation = explanation;
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

    public String getExplanation() {
        return explanation;
    }
}
