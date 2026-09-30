package com.resumeanalyzer.ai.prompt;

import java.util.Set;
import java.util.TreeSet;

/**
 * Centralized, versioned prompt template for the single Phase 5
 * analysis-enhancement call (advanced ATS insights, formatting/readability
 * insights, content suggestions, and interview questions, all generated
 * together in one request to keep latency/cost bounded to one Gemini call
 * per {@code POST /api/analysis}).
 *
 * Versioning: the template id below ({@link #VERSION}) is bumped whenever
 * the prompt wording or expected JSON shape changes, and is included in
 * logs (never in persisted data) purely for operational traceability -
 * "which prompt produced this behavior" is otherwise impossible to answer
 * once a template evolves.
 *
 * Inputs are truncated defensively (see {@link #MAX_RESUME_CHARS} /
 * {@link #MAX_JOB_DESCRIPTION_CHARS}) so a very large resume/job
 * description cannot blow up token usage or cost; matching/scoring itself
 * (Phase 3) is unaffected since it never uses this truncated copy.
 */
public final class AnalysisEnhancementPrompt {

    public static final String VERSION = "analysis-enhancement-v1";

    private static final int MAX_RESUME_CHARS = 6000;
    private static final int MAX_JOB_DESCRIPTION_CHARS = 3000;
    private static final int MAX_SKILLS_LISTED = 30;

    private AnalysisEnhancementPrompt() {
    }

    public static String build(String resumeText, String jobDescriptionText,
                                Set<String> matchedSkills, Set<String> missingSkills) {
        String resumeExcerpt = truncate(resumeText, MAX_RESUME_CHARS);
        String jobDescriptionExcerpt = truncate(jobDescriptionText, MAX_JOB_DESCRIPTION_CHARS);

        return """
                You are an expert technical recruiter and resume/ATS reviewer.

                Analyze the RESUME against the JOB DESCRIPTION below. You already know \
                which required skills were matched and which are missing (given below) - \
                do not re-derive or contradict that list.

                Respond with STRICT JSON ONLY, matching exactly this schema, and nothing \
                else (no markdown fences, no commentary before or after):

                {
                  "advancedAtsIssues": [string, ...],
                  "formattingInsights": [string, ...],
                  "contentSuggestions": [string, ...],
                  "interviewQuestions": [string, ...]
                }

                Field guidance:
                - advancedAtsIssues: ATS-parsing/keyword-optimization issues beyond basic \
                contact-info/section-heading checks (e.g. weak keyword density for the \
                target role, unclear job titles, missing quantifiable achievements). Return \
                at most 8 items, or an empty array if none apply.
                - formattingInsights: readability/formatting/writing-quality observations \
                (e.g. inconsistent tense, passive voice, overly dense paragraphs). Return at \
                most 8 items, or an empty array if none apply.
                - contentSuggestions: concrete, specific rewrite/addition suggestions tailored \
                to this resume and this job description (not generic advice). Return at most \
                10 items.
                - interviewQuestions: interview questions tailored to this candidate and role, \
                probing both their demonstrated strengths and the gaps versus the job \
                description. Return at most 10 items.

                Every string must be a complete, standalone sentence in plain English, under \
                50 words, with no PII beyond what is already in the resume/job description \
                text below, and no markdown formatting.

                ALREADY-MATCHED SKILLS: %s
                ALREADY-MISSING SKILLS: %s

                RESUME:
                %s

                JOB DESCRIPTION:
                %s
                """.formatted(
                joinSkills(matchedSkills),
                joinSkills(missingSkills),
                resumeExcerpt,
                jobDescriptionExcerpt);
    }

    private static String joinSkills(Set<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return "(none)";
        }
        Set<String> ordered = new TreeSet<>(skills);
        return String.join(", ", ordered.stream().limit(MAX_SKILLS_LISTED).toList());
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars) + " …(truncated)";
    }
}
