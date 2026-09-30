package com.resumeanalyzer.service.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates interview questions deterministically from the matching
 * data already computed by the Phase 3 matching engine (matched and
 * missing skills), using fixed templates - not a generative model. This
 * intentionally stays simple and explainable; richer, model-generated
 * interview questions are out of scope until the Gemini integration
 * phase.
 */
@Component
public class InterviewQuestionGenerator {

    public List<String> generate(Set<String> matchedSkills, Set<String> missingSkills) {
        List<String> questions = new ArrayList<>();

        if (matchedSkills != null) {
            for (String skill : new TreeSet<>(matchedSkills)) {
                questions.add("Can you describe a project where you applied \"" + skill + "\"?");
            }
        }

        if (missingSkills != null) {
            for (String skill : new TreeSet<>(missingSkills)) {
                questions.add("The role requires \"" + skill
                        + "\", which was not found in your resume - do you have relevant experience with it?");
            }
        }

        return questions;
    }
}
