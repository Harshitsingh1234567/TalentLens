import { apiClient } from './client';

/**
 * Wraps POST /api/analysis exactly as the backend defines it
 * (`AnalysisController`/`AnalysisRequest`): request
 * `{ resumeId, jobDescriptionId }`, both owned by the caller. Response
 * (201) is the full `AnalysisResponse` - score breakdown, matched/missing
 * skills, ATS/formatting issues, content suggestions, interview
 * questions, and the Phase 5 AI-enhanced fields + aiEnhancementStatus.
 */
export function createAnalysis({ resumeId, jobDescriptionId }) {
  return apiClient.post('/api/analysis', { resumeId, jobDescriptionId });
}

/**
 * Wraps GET /api/analysis/{id} - the same `AnalysisResponse` shape as
 * createAnalysis, for revisiting a previously created analysis.
 */
export function getAnalysis(analysisId) {
  return apiClient.get(`/api/analysis/${analysisId}`);
}

/**
 * Wraps GET /api/analysis/{id}/interview-questions - a read-only view of
 * just the interview questions already stored on that analysis
 * (`{ analysisId, questions, aiQuestions, aiEnhancementStatus }`). Used
 * where only the interview questions are wanted, without loading the
 * full analysis breakdown.
 */
export function getInterviewQuestions(analysisId) {
  return apiClient.get(`/api/analysis/${analysisId}/interview-questions`);
}
