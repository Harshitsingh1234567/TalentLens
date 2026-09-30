import { useCallback, useState } from 'react';
import { createAnalysis, getAnalysis } from '../api/analysisApi';
import { ApiError } from '../api/ApiError';

/**
 * Mirrors the one real constraint the backend places on these fields
 * (`@NotNull` on resumeId/jobDescriptionId in AnalysisRequest; the
 * analysis id in the GET path has no validation annotation, but a
 * non-numeric id is simply never going to resolve to a real row). This
 * only prevents an obviously-unusable request from being sent - whether
 * an id exists and is owned by the caller can only be decided by the
 * backend.
 */
function isValidId(value) {
  return /^\d+$/.test(value) && Number(value) > 0;
}

const INITIAL_STATE = {
  resumeId: '',
  jobDescriptionId: '',
  lookupAnalysisId: '',
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

/**
 * Backs both ways of getting an `AnalysisResponse` onto the page - running
 * a brand new analysis (`POST /api/analysis`) and loading a previously
 * created one (`GET /api/analysis/{id}`) - since they return the same
 * shape and the page displays either result identically. Kept as one
 * hook rather than two so that starting one action clears any result
 * from the other, and only one result is ever shown at a time.
 */
export function useAnalysisWorkflow() {
  const [state, setState] = useState(INITIAL_STATE);

  const setResumeId = useCallback((resumeId) => {
    setState((previous) => ({ ...previous, resumeId, status: 'idle', result: null, formError: null }));
  }, []);

  const setJobDescriptionId = useCallback((jobDescriptionId) => {
    setState((previous) => ({ ...previous, jobDescriptionId, status: 'idle', result: null, formError: null }));
  }, []);

  const setLookupAnalysisId = useCallback((lookupAnalysisId) => {
    setState((previous) => ({ ...previous, lookupAnalysisId, status: 'idle', result: null, formError: null }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const canRunAnalysis =
    isValidId(state.resumeId) && isValidId(state.jobDescriptionId) && state.status !== 'submitting';
  const canLoadAnalysis = isValidId(state.lookupAnalysisId) && state.status !== 'submitting';

  const runAnalysis = useCallback(async () => {
    if (!isValidId(state.resumeId) || !isValidId(state.jobDescriptionId) || state.status === 'submitting') {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await createAnalysis({
        resumeId: Number(state.resumeId),
        jobDescriptionId: Number(state.jobDescriptionId),
      });
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.resumeId, state.jobDescriptionId, state.status]);

  const loadAnalysis = useCallback(async () => {
    if (!isValidId(state.lookupAnalysisId) || state.status === 'submitting') {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await getAnalysis(Number(state.lookupAnalysisId));
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.lookupAnalysisId, state.status]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    canRunAnalysis,
    canLoadAnalysis,
    setResumeId,
    setJobDescriptionId,
    setLookupAnalysisId,
    runAnalysis,
    loadAnalysis,
    reset,
  };
}
