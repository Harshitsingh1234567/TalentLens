package com.resumeanalyzer.service.matching;

import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic text normalization shared by every matching/scoring
 * component, so resume and job-description text are always compared on
 * the same footing.
 *
 * Tokenization uses Apache OpenNLP's {@link SimpleTokenizer}, which is
 * rule-based and requires no trained model file (unlike OpenNLP's
 * sentence-detector or POS-tagger), keeping this fully offline and
 * deterministic. No NER, POS tagging, or other OpenNLP feature is used -
 * only the tokenization responsibility this phase actually needs.
 */
@Component
public class TextNormalizer {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "then", "of", "to", "in", "on", "for",
            "with", "as", "by", "at", "from", "is", "are", "was", "were", "be", "been", "being",
            "this", "that", "these", "those", "it", "its", "we", "you", "your", "our", "i",
            "will", "shall", "can", "could", "should", "would", "have", "has", "had", "do",
            "does", "did", "not", "no", "so", "than", "into", "about", "using", "use", "used"
    );

    /**
     * Normalizes raw text into a single lowercase string with collapsed
     * whitespace, suitable for substring/phrase matching (e.g. detecting a
     * multi-word skill like "spring boot"). Returns an empty string for
     * null/blank input - callers must not fabricate content for missing
     * text.
     */
    public String normalizeToPhraseText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(rawText.toLowerCase(Locale.ROOT));
        return String.join(" ", tokens).replaceAll("\\s+", " ").trim();
    }

    /**
     * Tokenizes and normalizes raw text into a deduplicated, ordered set of
     * meaningful terms: lowercased, stopwords removed, and single
     * (non-alphanumeric-only) characters dropped. Returns an empty set for
     * null/blank input.
     */
    public Set<String> normalizeToTermSet(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Set.of();
        }

        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(rawText.toLowerCase(Locale.ROOT));
        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> token.length() > 1)
                .filter(token -> token.chars().anyMatch(Character::isLetterOrDigit))
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
