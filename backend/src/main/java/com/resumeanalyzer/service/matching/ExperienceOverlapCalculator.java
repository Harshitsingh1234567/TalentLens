package com.resumeanalyzer.service.matching;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Computes the "experience" score component as a deterministic Jaccard
 * (intersection-over-union) overlap between the normalized term sets of
 * the resume and the job description.
 *
 * DESIGN NOTE / DECISION: the locked Phase 3 configuration names this
 * weight "experience", but true experience matching (years of experience,
 * seniority level, role history) would require structured resume section
 * segmentation, which was explicitly deferred out of Phase 2's scope
 * (raw-text extraction only - see ResumeParsingService). Until section
 * segmentation exists, this component is implemented as a general
 * keyword/term-overlap measure between the two documents, used as the
 * current deterministic stand-in for the "experience" weight slot. This
 * should be revisited once resume section segmentation is implemented in
 * a later phase.
 */
@Component
public class ExperienceOverlapCalculator {

    private final TextNormalizer textNormalizer;

    public ExperienceOverlapCalculator(TextNormalizer textNormalizer) {
        this.textNormalizer = textNormalizer;
    }

    public double overlap(String resumeText, String jobDescriptionText) {
        Set<String> resumeTerms = textNormalizer.normalizeToTermSet(resumeText);
        Set<String> jobDescriptionTerms = textNormalizer.normalizeToTermSet(jobDescriptionText);

        if (resumeTerms.isEmpty() || jobDescriptionTerms.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(resumeTerms);
        intersection.retainAll(jobDescriptionTerms);

        Set<String> union = new HashSet<>(resumeTerms);
        union.addAll(jobDescriptionTerms);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }
}
