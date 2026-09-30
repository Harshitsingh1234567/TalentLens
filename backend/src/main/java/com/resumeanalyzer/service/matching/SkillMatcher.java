package com.resumeanalyzer.service.matching;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Determines which dictionary skills a job description requires, which of
 * those the resume demonstrates, and which are missing.
 *
 * Matching is phrase-based (substring containment on normalized,
 * whitespace-collapsed text) so multi-word skills such as "spring boot" or
 * "machine learning" are matched correctly, not just single tokens.
 *
 * Both the input text AND every dictionary skill phrase are normalized
 * through the same {@link TextNormalizer} tokenization before comparison.
 * This matters for skills containing punctuation (e.g. "c++", ".net",
 * "node.js"), which the tokenizer splits into separate tokens - without
 * normalizing the dictionary side identically, those skills could never
 * match. The original, human-readable skill name is still what gets
 * reported in match results.
 */
@Component
public class SkillMatcher {

    private final TextNormalizer textNormalizer;

    /**
     * Original skill name -> its normalized phrase form, precomputed once
     * since the dictionary is fixed for the lifetime of the application.
     */
    private final Map<String, String> normalizedSkillsByOriginal;

    public SkillMatcher(TextNormalizer textNormalizer, SkillDictionary skillDictionary) {
        this.textNormalizer = textNormalizer;
        this.normalizedSkillsByOriginal = new LinkedHashMap<>();
        for (String skill : skillDictionary.getSkills()) {
            String normalized = textNormalizer.normalizeToPhraseText(skill);
            if (!normalized.isEmpty()) {
                this.normalizedSkillsByOriginal.put(skill, normalized);
            }
        }
    }

    /**
     * The result of matching a resume against a job description's
     * required skills.
     */
    public record SkillMatchResult(Set<String> requiredSkills, Set<String> matchedSkills, Set<String> missingSkills) {
    }

    public SkillMatchResult match(String resumeText, String jobDescriptionText) {
        String normalizedResume = textNormalizer.normalizeToPhraseText(resumeText);
        String normalizedJobDescription = textNormalizer.normalizeToPhraseText(jobDescriptionText);

        Set<String> requiredSkills = findSkillsPresentIn(normalizedJobDescription);
        Set<String> resumeSkills = findSkillsPresentIn(normalizedResume);

        Set<String> matched = new TreeSet<>();
        Set<String> missing = new TreeSet<>();
        for (String skill : requiredSkills) {
            if (resumeSkills.contains(skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        return new SkillMatchResult(new TreeSet<>(requiredSkills), matched, missing);
    }

    private Set<String> findSkillsPresentIn(String normalizedPhraseText) {
        Set<String> present = new TreeSet<>();
        if (normalizedPhraseText.isEmpty()) {
            return present;
        }

        for (Map.Entry<String, String> entry : normalizedSkillsByOriginal.entrySet()) {
            if (containsPhrase(normalizedPhraseText, entry.getValue())) {
                present.add(entry.getKey());
            }
        }
        return present;
    }

    /**
     * Whether {@code text} contains {@code phrase} as a whole-word/phrase
     * match rather than as an arbitrary substring (e.g. "java" must not
     * match inside "javascript").
     */
    private boolean containsPhrase(String text, String phrase) {
        String padded = " " + text + " ";
        String paddedPhrase = " " + phrase + " ";
        return padded.contains(paddedPhrase);
    }
}
