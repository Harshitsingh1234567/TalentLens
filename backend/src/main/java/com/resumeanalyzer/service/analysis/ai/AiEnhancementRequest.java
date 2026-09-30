package com.resumeanalyzer.service.analysis.ai;

import java.util.Set;

/**
 * The (Resume, JobDescription, Phase-3-matching-result) context an
 * {@link AiAnalysisEnhancer} needs to produce context-aware output. Plain
 * data - no persistence or HTTP concerns - so it is trivial to construct
 * in tests without touching Spring or JPA.
 */
public record AiEnhancementRequest(String resumeText,
                                    String jobDescriptionText,
                                    Set<String> matchedSkills,
                                    Set<String> missingSkills) {
}
