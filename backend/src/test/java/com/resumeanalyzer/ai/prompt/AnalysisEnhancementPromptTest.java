package com.resumeanalyzer.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisEnhancementPromptTest {

    @Test
    void includesResumeJobDescriptionAndSkillsInTheGeneratedPrompt() {
        String prompt = AnalysisEnhancementPrompt.build(
                "Experienced Java developer.",
                "Looking for a Java engineer.",
                Set.of("java"),
                Set.of("aws"));

        assertTrue(prompt.contains("Experienced Java developer."));
        assertTrue(prompt.contains("Looking for a Java engineer."));
        assertTrue(prompt.contains("java"));
        assertTrue(prompt.contains("aws"));
        assertTrue(prompt.contains("STRICT JSON"));
    }

    @Test
    void handlesNullSkillsAndBlankTextWithoutThrowing() {
        String prompt = AnalysisEnhancementPrompt.build(null, null, null, null);

        assertTrue(prompt.contains("(none)"));
    }

    @Test
    void truncatesAnOverlyLongResumeRatherThanIncludingItInFull() {
        String hugeResume = "x".repeat(10_000);

        String prompt = AnalysisEnhancementPrompt.build(hugeResume, "job description", Set.of(), Set.of());

        assertTrue(prompt.contains("truncated"));
        assertTrue(prompt.length() < hugeResume.length() + 2000);
    }
}
