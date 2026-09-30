package com.resumeanalyzer.service.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterviewQuestionGeneratorTest {

    private final InterviewQuestionGenerator generator = new InterviewQuestionGenerator();

    @Test
    void generatesQuestionsForMatchedAndMissingSkills() {
        List<String> questions = generator.generate(Set.of("java"), Set.of("aws"));

        assertEquals(2, questions.size());
        assertTrue(questions.stream().anyMatch(q -> q.contains("java")));
        assertTrue(questions.stream().anyMatch(q -> q.contains("aws")));
    }

    @Test
    void returnsEmptyListWhenNoSkillsAtAll() {
        assertTrue(generator.generate(Set.of(), Set.of()).isEmpty());
    }

    @Test
    void handlesNullSkillSetsSafely() {
        assertTrue(generator.generate(null, null).isEmpty());
    }
}
