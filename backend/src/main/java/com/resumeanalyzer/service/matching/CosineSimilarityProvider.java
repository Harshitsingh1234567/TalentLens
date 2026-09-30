package com.resumeanalyzer.service.matching;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Default {@link SimilarityProvider}: cosine similarity between the
 * term-frequency vectors of two normalized texts.
 *
 * This is a standard, well-understood information-retrieval technique
 * (bag-of-words cosine similarity), computed entirely locally and
 * deterministically - no external service, no network call, no
 * fabricated score. It serves as the current "semantic" component of the
 * match score until a true embeddings-based provider (Gemini) is
 * introduced in a later phase.
 */
@Component
public class CosineSimilarityProvider implements SimilarityProvider {

    private final TextNormalizer textNormalizer;

    public CosineSimilarityProvider(TextNormalizer textNormalizer) {
        this.textNormalizer = textNormalizer;
    }

    @Override
    public double similarity(String textA, String textB) {
        Set<String> termsA = textNormalizer.normalizeToTermSet(textA);
        Set<String> termsB = textNormalizer.normalizeToTermSet(textB);

        if (termsA.isEmpty() || termsB.isEmpty()) {
            return 0.0;
        }

        Map<String, Integer> vectorA = termFrequencies(termsA);
        Map<String, Integer> vectorB = termFrequencies(termsB);

        Set<String> vocabulary = new HashSet<>(vectorA.keySet());
        vocabulary.addAll(vectorB.keySet());

        long dotProduct = 0;
        long magnitudeA = 0;
        long magnitudeB = 0;

        for (String term : vocabulary) {
            int a = vectorA.getOrDefault(term, 0);
            int b = vectorB.getOrDefault(term, 0);
            dotProduct += (long) a * b;
            magnitudeA += (long) a * a;
            magnitudeB += (long) b * b;
        }

        if (magnitudeA == 0 || magnitudeB == 0) {
            return 0.0;
        }

        double denominator = Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB);
        return dotProduct / denominator;
    }

    /**
     * Since {@link TextNormalizer#normalizeToTermSet} already deduplicates
     * terms, every present term has a frequency of 1 - this still models a
     * proper term-frequency vector (a presence/absence vector), keeping
     * the cosine-similarity computation correct and simple.
     */
    private Map<String, Integer> termFrequencies(Set<String> terms) {
        Map<String, Integer> frequencies = new HashMap<>();
        for (String term : terms) {
            frequencies.put(term, 1);
        }
        return frequencies;
    }
}
