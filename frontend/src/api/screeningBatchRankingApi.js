import { apiClient } from './client';

/**
 * Wraps POST /api/screening-batches/{id}/analyze exactly as the backend
 * defines it (`ScreeningBatchController`/`ScreeningBatchAnalyzeRequest`):
 * request `{ jobDescriptionId }`. Response (201) is the same
 * `ScreeningBatchRankingResponse` shape as getScreeningBatchRanking.
 * Re-running this against the same batch replaces its previous ranking
 * (server-side behavior, not something this client needs to account
 * for).
 */
export function analyzeScreeningBatch(batchId, { jobDescriptionId }) {
  return apiClient.post(`/api/screening-batches/${batchId}/analyze`, { jobDescriptionId });
}

/**
 * Wraps GET /api/screening-batches/{id}/ranking - the current ranking
 * for a batch that has already been analyzed at least once.
 * `{ screeningBatchId, jobDescriptionId, analyzedAt, candidatesRanked,
 *    rankings: [{rank, resumeId, candidateName, finalScore, skillScore,
 *    experienceScore, semanticScore, matchedSkills, missingSkills}] }`.
 * `rankings` is already in the backend's final rank order (descending
 * score, ties broken by resume id) - never re-sorted here.
 */
export function getScreeningBatchRanking(batchId) {
  return apiClient.get(`/api/screening-batches/${batchId}/ranking`);
}
