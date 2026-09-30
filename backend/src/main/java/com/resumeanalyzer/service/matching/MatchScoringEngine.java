package com.resumeanalyzer.service.matching;

import com.resumeanalyzer.config.MatchingWeightsProperties;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * The Resume &lt;-&gt; JobDescription matching/scoring engine.
 *
 * This is a plain, framework-agnostic (beyond dependency injection)
 * service with no HTTP or persistence concerns, so it is directly
 * reusable by any future workflow that needs a match score - the planned
 * Job Seeker single-analysis flow and the Recruiter screening/ranking
 * flow alike - without duplicating scoring logic.
 *
 * Score components:
 *   - skills: matched-skills / required-skills ratio (see SkillMatcher)
 *   - experience: term-overlap ratio between resume and JD text (see
 *     ExperienceOverlapCalculator; a documented deterministic stand-in
 *     pending real experience-section extraction)
 *   - semantic: cosine similarity of term-frequency vectors (see
 *     SimilarityProvider / CosineSimilarityProvider; a documented
 *     deterministic stand-in pending Gemini-embeddings integration)
 *
 * finalScore = weights.skills * skillScore
 *            + weights.experience * experienceScore
 *            + weights.semantic * semanticScore
 *
 * All inputs and outputs are plain, reproducible functions of the given
 * text - no randomness, no external calls, no fabricated values. Null or
 * blank resume/job-description text safely yields zero-valued components
 * rather than a fabricated score.
 */
@Service
public class MatchScoringEngine {

    private final SkillMatcher skillMatcher;
    private final ExperienceOverlapCalculator experienceOverlapCalculator;
    private final SimilarityProvider similarityProvider;
    private final MatchingWeightsProperties weights;

    public MatchScoringEngine(SkillMatcher skillMatcher,
                               ExperienceOverlapCalculator experienceOverlapCalculator,
                               SimilarityProvider similarityProvider,
                               MatchingWeightsProperties weights) {
        this.skillMatcher = skillMatcher;
        this.experienceOverlapCalculator = experienceOverlapCalculator;
        this.similarityProvider = similarityProvider;
        this.weights = weights;
    }

    /**
     * The full, explainable result of scoring a resume against a job
     * description.
     */
    public record MatchScoreResult(Set<String> matchedSkills,
                                    Set<String> missingSkills,
                                    double skillScore,
                                    double experienceScore,
                                    double semanticScore,
                                    double finalScore,
                                    String explanation) {
    }

    public MatchScoreResult score(String resumeText, String jobDescriptionText) {
        SkillMatcher.SkillMatchResult skillMatch = skillMatcher.match(resumeText, jobDescriptionText);
        double skillScore = computeSkillScore(skillMatch);

        double experienceScore = experienceOverlapCalculator.overlap(resumeText, jobDescriptionText);
        double semanticScore = similarityProvider.similarity(resumeText, jobDescriptionText);

        double finalScore = (weights.getSkills() * skillScore)
                + (weights.getExperience() * experienceScore)
                + (weights.getSemantic() * semanticScore);

        String explanation = buildExplanation(skillMatch, skillScore, experienceScore, semanticScore, finalScore);

        return new MatchScoreResult(
                skillMatch.matchedSkills(),
                skillMatch.missingSkills(),
                round(skillScore),
                round(experienceScore),
                round(semanticScore),
                round(finalScore),
                explanation);
    }

    private double computeSkillScore(SkillMatcher.SkillMatchResult skillMatch) {
        if (skillMatch.requiredSkills().isEmpty()) {
            return 0.0;
        }
        return (double) skillMatch.matchedSkills().size() / skillMatch.requiredSkills().size();
    }

    private String buildExplanation(SkillMatcher.SkillMatchResult skillMatch,
                                     double skillScore,
                                     double experienceScore,
                                     double semanticScore,
                                     double finalScore) {
        StringBuilder explanation = new StringBuilder();

        if (skillMatch.requiredSkills().isEmpty()) {
            explanation.append("No recognized skills were found in the job description, so the skill component is 0%. ");
        } else {
            explanation.append("Skill match: ").append(skillMatch.matchedSkills().size())
                    .append(" of ").append(skillMatch.requiredSkills().size())
                    .append(" required skills matched (").append(toPercent(skillScore)).append("%). ");
        }

        explanation.append("Experience/keyword overlap: ").append(toPercent(experienceScore)).append("%. ");
        explanation.append("Semantic similarity: ").append(toPercent(semanticScore)).append("%. ");
        explanation.append("Weighted final score: ").append(toPercent(finalScore)).append("%.");

        return explanation.toString();
    }

    private long toPercent(double fraction) {
        return Math.round(fraction * 100);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
