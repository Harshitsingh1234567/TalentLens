package com.resumeanalyzer.service.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosineSimilarityProviderTest {

    private final CosineSimilarityProvider provider = new CosineSimilarityProvider(new TextNormalizer());

    @Test
    void identicalTextsHaveMaximumSimilarity() {
        String text = "Experienced backend engineer with strong Java and Spring Boot skills";

        double similarity = provider.similarity(text, text);

        assertEquals(1.0, similarity, 0.0001);
    }

    @Test
    void completelyDifferentTextsHaveZeroSimilarity() {
        double similarity = provider.similarity("apple banana orange", "guitar drum piano");

        assertEquals(0.0, similarity, 0.0001);
    }

    @Test
    void partiallyOverlappingTextsHaveIntermediateSimilarity() {
        double similarity = provider.similarity(
                "Java Spring Boot developer with Docker experience",
                "Java developer with Kubernetes and AWS experience");

        assertTrue(similarity > 0.0 && similarity < 1.0);
    }

    @Test
    void nullOrBlankInputYieldsZeroSimilarity() {
        assertEquals(0.0, provider.similarity(null, "Java developer"), 0.0001);
        assertEquals(0.0, provider.similarity("Java developer", "   "), 0.0001);
        assertEquals(0.0, provider.similarity(null, null), 0.0001);
    }

    @Test
    void similarityIsDeterministicAcrossRepeatedCalls() {
        String textA = "Full stack engineer skilled in React and Node.js";
        String textB = "Looking for a React and Node.js full stack engineer";

        double first = provider.similarity(textA, textB);
        double second = provider.similarity(textA, textB);

        assertEquals(first, second, 0.0);
    }
}
