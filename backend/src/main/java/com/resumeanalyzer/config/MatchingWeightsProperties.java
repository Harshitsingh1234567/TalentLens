package com.resumeanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the weighted MatchScore formula:
 *
 *   MatchScore = (weights.skills * skillScore)
 *              + (weights.experience * experienceScore)
 *              + (weights.semantic * semanticScore)
 *
 * Binds to the "matching.weights" prefix in application.yml. These values
 * are intentionally kept as configuration rather than hard-coded constants
 * so they can be tuned without changing service code.
 *
 * Phase 0 introduced this configuration holder as infrastructure only,
 * with placeholder field names (skillMatch/keywordOverlap/semanticSimilarity).
 * Phase 3 is the first consumer of these weights (via MatchScoringEngine)
 * and renames the fields to match the locked Phase 3 configuration keys
 * (skills/experience/semantic) so the property names accurately reflect
 * what each weight is actually applied to.
 */
@ConfigurationProperties(prefix = "matching.weights")
public class MatchingWeightsProperties {

    /**
     * Weight applied to the skill-match ratio (matched skills / JD skills).
     */
    private double skills = 0.34;

    /**
     * Weight applied to the experience/keyword-overlap ratio between
     * resume and job description text.
     */
    private double experience = 0.33;

    /**
     * Weight applied to the semantic similarity score, computed via the
     * pluggable {@link com.resumeanalyzer.service.matching.SimilarityProvider}
     * abstraction (a local deterministic implementation for now; Gemini
     * embeddings are introduced in a later phase).
     */
    private double semantic = 0.33;

    public double getSkills() {
        return skills;
    }

    public void setSkills(double skills) {
        this.skills = skills;
    }

    public double getExperience() {
        return experience;
    }

    public void setExperience(double experience) {
        this.experience = experience;
    }

    public double getSemantic() {
        return semantic;
    }

    public void setSemantic(double semantic) {
        this.semantic = semantic;
    }
}
