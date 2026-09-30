import { apiClient } from './client';

/**
 * Wraps POST /api/screening-batches exactly as the backend defines it
 * (`ScreeningBatchController`): multipart/form-data with a required
 * "name" field and one or more "files" parts. Response (201):
 * `{ id, name, createdAt, resumes: [{resumeId, originalFilename,
 *    fileType, candidateName, uploadedAt}], failedFiles: [{originalFilename,
 *    reason}] }`. A batch is created even when some files fail to
 * parse - `failedFiles` reports those without aborting the whole
 * request; only a request-level problem (blank name, no files, too many
 * files) is rejected outright with nothing persisted.
 */
export function createScreeningBatch({ name, files }) {
  const formData = new FormData();
  formData.append('name', name);
  files.forEach((file) => formData.append('files', file));
  return apiClient.post('/api/screening-batches', formData);
}

/**
 * Wraps GET /api/screening-batches/{id} - the same response shape as
 * createScreeningBatch, for revisiting a previously created batch.
 * `failedFiles` is always empty here (a failed file was never
 * persisted, so there is nothing left to report on a GET).
 */
export function getScreeningBatch(batchId) {
  return apiClient.get(`/api/screening-batches/${batchId}`);
}
