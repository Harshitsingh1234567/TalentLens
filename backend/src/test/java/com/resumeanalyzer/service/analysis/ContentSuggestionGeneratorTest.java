package com.resumeanalyzer.service.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentSuggestionGeneratorTest {

    private final ContentSuggestionGenerator generator = new ContentSuggestionGenerator();

    @Test
    void generatesOneSuggestionPerMissingSkill() {
        List<String> suggestions = generator.generate(Set.of("java", "aws"));

        assertEquals(2, suggestions.size());
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("java")));
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("aws")));
    }

    @Test
    void returnsEmptyListForNoMissingSkills() {
        assertTrue(generator.generate(Set.of()).isEmpty());
    }

    @Test
    void returnsEmptyListForNullMissingSkills() {
        assertTrue(generator.generate(null).isEmpty());
    }
}
