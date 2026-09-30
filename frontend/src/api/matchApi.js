import { apiClient } from './client';

/**
 * Wraps POST /api/matches exactly as the backend defines it
 * (`MatchController`/`MatchRequest`): request `{ resumeId, jobDescriptionId }`,
 * both of which must belong to the authenticated caller (enforced by
 * `MatchService`, not by this client). Response (200):
 * `{ resumeId, jobDescriptionId, matchedSkills, missingSkills, skillScore,
 *    experienceScore, semanticScore, finalScore, explanation }`.
 */
export function matchResumeToJobDescription({ resumeId, jobDescriptionId }) {
  return apiClient.post('/api/matches', { resumeId, jobDescriptionId });
}
