package com.resumeanalyzer.dto;

import java.util.Set;

/**
 * One ranked candidate within a {@link ScreeningBatchRankingResponse}
 * (Phase 7).
 *
 * {@code rank} is 1-based and reflects descending {@code finalScore}
 * (ties broken by resume id, ascending, for a deterministic order - see
 * {@code ScreeningBatchRankingService}). {@code resumeId} identifies the
 * underlying {@code Resume}; there is no separate Candidate id, per the
 * locked Phase 6 design (a candidate in a batch simply *is* a Resume).
 * Score components (skill/experience/semantic) are exposed individually,
 * not just the blended {@code finalScore}, for the same explainability
 * reason as {@code MatchResponse} (Phase 3).
 */
public record CandidateRankingEntryResponse(int rank,
                                              Long resumeId,
                                              String candidateName,
                                              double finalScore,
                                              double skillScore,
                                              double experienceScore,
                                              double semanticScore,
                                              Set<String> matchedSkills,
                                              Set<String> missingSkills) {
}
