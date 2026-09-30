package com.resumeanalyzer.service.analysis;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtsAnalyzerTest {

    private final AtsAnalyzer atsAnalyzer = new AtsAnalyzer();

    @Test
    void flagsMissingEmailAndPhone() {
        String resume = "Experienced software engineer with strong Java skills and Experience section.";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of("java"), Set.of("java"));

        assertTrue(result.atsIssues().stream().anyMatch(issue -> issue.contains("email")));
        assertTrue(result.atsIssues().stream().anyMatch(issue -> issue.contains("phone")));
    }

    @Test
    void doesNotFlagEmailOrPhoneWhenPresent() {
        String resume = "Contact: jane.doe@example.com, +1 555-123-4567. "
                + "Experience: 5 years of backend development. Education: BS Computer Science. Skills: Java.";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of("java"), Set.of("java"));

        assertFalse(result.atsIssues().stream().anyMatch(issue -> issue.contains("email")));
        assertFalse(result.atsIssues().stream().anyMatch(issue -> issue.contains("phone")));
    }

    @Test
    void flagsMissingStandardSectionHeadings() {
        String resume = "jane.doe@example.com +1 555-123-4567 I build software and solve problems every day at work.";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of(), Set.of());

        assertTrue(result.atsIssues().stream().anyMatch(issue -> issue.contains("section headings")));
    }

    @Test
    void flagsVeryShortResume() {
        String resume = "Java developer.";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of(), Set.of());

        assertTrue(result.atsIssues().stream().anyMatch(issue -> issue.contains("very short")));
    }

    @Test
    void flagsZeroSkillMatchWhenJobHasRequiredSkills() {
        String resume = "Experience: worked as a chef for many years. Education: culinary school. Skills: cooking.";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of("java", "python"), Set.of());

        assertTrue(result.atsIssues().stream().anyMatch(issue -> issue.contains("None of the job description")));
    }

    @Test
    void doesNotFlagSkillMismatchWhenJobHasNoRequiredSkills() {
        String resume = "Experience: general work history. Education: degree. Skills: various.";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of(), Set.of());

        assertFalse(result.atsIssues().stream().anyMatch(issue -> issue.contains("None of the job description")));
    }

    @Test
    void flagsExcessiveUppercase() {
        String resume = "JANE DOE SOFTWARE ENGINEER EXPERIENCE EDUCATION SKILLS JAVA PYTHON DOCKER KUBERNETES "
                + "jane@example.com 555-123-4567";

        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(resume, Set.of(), Set.of());

        assertTrue(result.formattingIssues().stream().anyMatch(issue -> issue.contains("capital letters")));
    }

    @Test
    void handlesBlankResumeSafely() {
        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze("   ", Set.of(), Set.of());

        assertTrue(result.atsIssues().size() == 1);
        assertTrue(result.formattingIssues().isEmpty());
    }

    @Test
    void handlesNullResumeSafely() {
        AtsAnalyzer.AtsAnalysisResult result = atsAnalyzer.analyze(null, Set.of(), Set.of());

        assertFalse(result.atsIssues().isEmpty());
    }
}
