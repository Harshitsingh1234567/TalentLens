package com.resumeanalyzer.service.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates rule-based content suggestions strictly from the set of
 * missing skills already computed by the Phase 3 matching engine - no
 * generative AI, no invented advice beyond a fixed template applied to
 * data that is already known to be true (a skill the job description
 * asks for was not found in the resume).
 */
@Component
public class ContentSuggestionGenerator {

    public List<String> generate(Set<String> missingSkills) {
        List<String> suggestions = new ArrayList<>();
        if (missingSkills == null || missingSkills.isEmpty()) {
            return suggestions;
        }

        for (String skill : new TreeSet<>(missingSkills)) {
            suggestions.add("Consider adding concrete experience or projects demonstrating \"" + skill
                    + "\", since the job description requires it but it was not found in the resume.");
        }

        return suggestions;
    }
}
