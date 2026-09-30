import { apiClient } from './client';

/**
 * Wraps POST /api/resumes exactly as the backend defines it
 * (`ResumeController`): multipart/form-data with a required "file" part
 * and an optional "candidateName" field. Response (201):
 * `{ id, originalFilename, fileType, candidateName, uploadedAt, extractedText }`.
 */
export function uploadResume({ file, candidateName }) {
  const formData = new FormData();
  formData.append('file', file);
  if (candidateName) {
    formData.append('candidateName', candidateName);
  }
  return apiClient.post('/api/resumes', formData);
}

/**
 * Wraps GET /api/resumes exactly as the backend defines it
 * (`ResumeController`): the authenticated caller's own resumes only,
 * most recently uploaded first. Response (200):
 * `[{ id, originalFilename, uploadedAt }, ...]` - deliberately a smaller
 * shape than the upload response (no `extractedText`), matching what
 * `ResumeSummaryResponse` actually returns.
 */
export function listResumes() {
  return apiClient.get('/api/resumes');
}
