import { apiClient } from './client';

/**
 * Wraps POST /api/job-descriptions exactly as the backend defines it
 * (`JobDescriptionController`): request `{ content }`. Response (201):
 * `{ id, content, submittedAt }`.
 */
export function submitJobDescription({ content }) {
  return apiClient.post('/api/job-descriptions', { content });
}

/**
 * Wraps GET /api/job-descriptions exactly as the backend defines it
 * (`JobDescriptionController`): the authenticated caller's own job
 * descriptions only, most recently submitted first. Response (200):
 * `[{ id, contentPreview, submittedAt }, ...]` - `contentPreview` is a
 * server-truncated excerpt of the full content (see
 * `JobDescriptionSummaryResponse`), not the full submitted text.
 */
export function listJobDescriptions() {
  return apiClient.get('/api/job-descriptions');
}
