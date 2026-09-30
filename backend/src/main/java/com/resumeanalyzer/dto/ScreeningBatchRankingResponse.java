package com.resumeanalyzer.dto;

import java.time.Instant;
import java.util.List;

/**
 * The response for both POST /api/screening-batches/{id}/analyze
 * (run/replace the ranking) and GET /api/screening-batches/{id}/ranking
 * (retrieve the current one) - the same single-DTO-per-resource
 * convention already used by {@code ScreeningBatchResponse} (Phase 6),
 * since both endpoints describe exactly the same underlying resource.
 *
 * {@code candidatesRanked} and an empty {@code rankings} list are the
 * well-defined, deterministic result of analyzing a batch with no
 * successfully-processed resumes (an empty batch, or one where every
 * file failed to parse) - not an error condition; see
 * {@code ScreeningBatchRankingService}.
 */
public record ScreeningBatchRankingResponse(Long screeningBatchId,
                                             Long jobDescriptionId,
                                             Instant analyzedAt,
                                             int candidatesRanked,
                                             List<CandidateRankingEntryResponse> rankings) {
}
