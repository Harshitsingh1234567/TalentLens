package com.resumeanalyzer.service.matching;

/**
 * Computes a semantic similarity score between two pieces of text, in the
 * range [0.0, 1.0].
 *
 * This abstraction exists so the matching engine's "semantic" score
 * component is decoupled from how similarity is actually computed. The
 * current implementation ({@link CosineSimilarityProvider}) is a local,
 * deterministic, dependency-free calculation - it does NOT call any
 * external API. Gemini-embeddings-based semantic similarity is explicitly
 * out of scope for this phase and belongs to the later Gemini
 * integration phase; when that arrives, it will plug in as an additional
 * implementation of this interface without changing
 * {@link MatchScoringEngine} or any other caller.
 */
public interface SimilarityProvider {

    /**
     * Returns a similarity score in [0.0, 1.0] between the two texts.
     * Implementations must be deterministic: the same inputs always
     * produce the same output.
     */
    double similarity(String textA, String textB);
}
