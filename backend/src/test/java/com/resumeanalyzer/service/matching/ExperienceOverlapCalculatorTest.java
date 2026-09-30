package com.resumeanalyzer.service.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceOverlapCalculatorTest {

    private final ExperienceOverlapCalculator calculator = new ExperienceOverlapCalculator(new TextNormalizer());

    @Test
    void identicalTextsHaveFullOverlap() {
        String text = "Senior backend engineer with distributed systems experience";

        assertEquals(1.0, calculator.overlap(text, text), 0.0001);
    }

    @Test
    void disjointTextsHaveZeroOverlap() {
        assertEquals(0.0, calculator.overlap("apple banana orange", "guitar drum piano"), 0.0001);
    }

    @Test
    void partialOverlapIsBetweenZeroAndOne() {
        double overlap = calculator.overlap(
                "Backend engineer with Java and distributed systems experience",
                "Looking for a Java engineer with cloud experience");

        assertTrue(overlap > 0.0 && overlap < 1.0);
    }

    @Test
    void nullOrBlankInputYieldsZeroOverlap() {
        assertEquals(0.0, calculator.overlap(null, "some text"), 0.0001);
        assertEquals(0.0, calculator.overlap("some text", ""), 0.0001);
    }
}
