package com.resumeanalyzer.service.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillMatcherTest {

    private SkillMatcher skillMatcher;

    @BeforeEach
    void setUp() {
        TextNormalizer textNormalizer = new TextNormalizer();
        SkillDictionary skillDictionary = new SkillDictionary();
        skillMatcher = new SkillMatcher(textNormalizer, skillDictionary);
    }

    @Test
    void matchesSkillsPresentInBothTexts() {
        String resume = "Experienced engineer skilled in Java, Spring Boot and Docker.";
        String jobDescription = "Looking for a Java and Spring Boot developer with AWS experience.";

        SkillMatcher.SkillMatchResult result = skillMatcher.match(resume, jobDescription);

        assertTrue(result.matchedSkills().contains("java"));
        assertTrue(result.matchedSkills().contains("spring boot"));
        assertTrue(result.missingSkills().contains("aws"));
        assertEquals(result.requiredSkills().size(), result.matchedSkills().size() + result.missingSkills().size());
    }

    @Test
    void matchesPunctuationBearingSkills() {
        String resume = "Backend developer proficient in C++ and Node.js.";
        String jobDescription = "Seeking a C++ developer, Node.js experience is a plus.";

        SkillMatcher.SkillMatchResult result = skillMatcher.match(resume, jobDescription);

        assertTrue(result.matchedSkills().contains("c++"));
        assertTrue(result.matchedSkills().contains("node.js"));
    }

    @Test
    void doesNotFalsePositiveMatchJavaInsideJavascript() {
        String resume = "Frontend engineer specializing in JavaScript.";
        String jobDescription = "Looking for a JavaScript developer.";

        SkillMatcher.SkillMatchResult result = skillMatcher.match(resume, jobDescription);

        assertTrue(result.requiredSkills().contains("javascript"));
        assertTrue(result.matchedSkills().contains("javascript"));
        // "java" itself must not be reported as required/matched merely
        // because it is a substring of "javascript".
        assertFalse(result.requiredSkills().contains("java"));
    }

    @Test
    void returnsEmptyResultForBlankJobDescription() {
        SkillMatcher.SkillMatchResult result = skillMatcher.match("Java developer", "   ");

        assertEquals(0, result.requiredSkills().size());
        assertEquals(0, result.matchedSkills().size());
        assertEquals(0, result.missingSkills().size());
    }

    @Test
    void returnsAllRequiredSkillsAsMissingWhenResumeIsBlank() {
        SkillMatcher.SkillMatchResult result = skillMatcher.match(null, "Java and Python developer needed.");

        assertTrue(result.missingSkills().contains("java"));
        assertTrue(result.missingSkills().contains("python"));
        assertEquals(0, result.matchedSkills().size());
    }
}
