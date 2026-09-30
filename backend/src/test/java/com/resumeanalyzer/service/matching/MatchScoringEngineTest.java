package com.resumeanalyzer.service.matching;

import com.resumeanalyzer.config.MatchingWeightsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScoringEngineTest {

    private MatchScoringEngine engine;
    private MatchingWeightsProperties weights;

    @BeforeEach
    void setUp() {
        TextNormalizer textNormalizer = new TextNormalizer();
        SkillMatcher skillMatcher = new SkillMatcher(textNormalizer, new SkillDictionary());
        ExperienceOverlapCalculator experienceOverlapCalculator = new ExperienceOverlapCalculator(textNormalizer);
        CosineSimilarityProvider similarityProvider = new CosineSimilarityProvider(textNormalizer);

        weights = new MatchingWeightsProperties();
        weights.setSkills(0.34);
        weights.setExperience(0.33);
        weights.setSemantic(0.33);

        engine = new MatchScoringEngine(skillMatcher, experienceOverlapCalculator, similarityProvider, weights);
    }

    @Test
    void finalScoreIsWeightedSumOfComponents() {
        String resume = "Java and Spring Boot backend developer with Docker experience";
        String jobDescription = "Seeking a Java and Spring Boot developer with AWS experience";

        MatchScoringEngine.MatchScoreResult result = engine.score(resume, jobDescription);

        double expected = (weights.getSkills() * result.skillScore())
                + (weights.getExperience() * result.experienceScore())
                + (weights.getSemantic() * result.semanticScore());

        assertEquals(Math.round(expected * 10000.0) / 10000.0, result.finalScore(), 0.0001);
    }

    @Test
    void resultIsReproducibleForTheSameInput() {
        String resume = "Python developer with machine learning experience";
        String jobDescription = "Looking for a Python and machine learning engineer";

        MatchScoringEngine.MatchScoreResult first = engine.score(resume, jobDescription);
        MatchScoringEngine.MatchScoreResult second = engine.score(resume, jobDescription);

        assertEquals(first.finalScore(), second.finalScore(), 0.0);
        assertEquals(first.matchedSkills(), second.matchedSkills());
    }

    @Test
    void reportsMatchedAndMissingSkillsSeparately() {
        String resume = "Java developer";
        String jobDescription = "Java and Kubernetes engineer needed";

        MatchScoringEngine.MatchScoreResult result = engine.score(resume, jobDescription);

        assertTrue(result.matchedSkills().contains("java"));
        assertTrue(result.missingSkills().contains("kubernetes"));
        assertTrue(result.explanation().contains("Skill match"));
    }

    @Test
    void blankResumeAndJobDescriptionYieldZeroScoreWithoutError() {
        MatchScoringEngine.MatchScoreResult result = engine.score("", "");

        assertEquals(0.0, result.skillScore(), 0.0001);
        assertEquals(0.0, result.experienceScore(), 0.0001);
        assertEquals(0.0, result.semanticScore(), 0.0001);
        assertEquals(0.0, result.finalScore(), 0.0001);
        assertTrue(result.matchedSkills().isEmpty());
        assertTrue(result.missingSkills().isEmpty());
    }

    @Test
    void nullInputsAreHandledSafely() {
        MatchScoringEngine.MatchScoreResult result = engine.score(null, null);

        assertEquals(0.0, result.finalScore(), 0.0001);
    }
}
