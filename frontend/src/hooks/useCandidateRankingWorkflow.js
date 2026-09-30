import { useCallback, useState } from 'react';
import { analyzeScreeningBatch, getScreeningBatchRanking } from '../api/screeningBatchRankingApi';
import { ApiError } from '../api/ApiError';

function isValidId(value) {
  return /^\d+$/.test(value) && Number(value) > 0;
}

const INITIAL_STATE = {
  batchId: '',
  jobDescriptionId: '',
  lookupBatchId: '',
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

/**
 * Backs both ways of getting a `ScreeningBatchRankingResponse` onto the
 * page - running/replacing the ranking for a batch
 * (`POST /api/screening-batches/{id}/analyze`) and loading the current
 * ranking for one already analyzed (`GET /api/screening-batches/{id}/ranking`)
 * - since they return the same shape and the page displays either result
 * identically. Starting one action clears any result from the other.
 *
 * `rankings` from either call is rendered exactly as received; this hook
 * never sorts, filters, or otherwise reorders it - the backend's rank
 * order and tie-breaking are authoritative.
 */
export function useCandidateRankingWorkflow() {
  const [state, setState] = useState(INITIAL_STATE);

  const setBatchId = useCallback((batchId) => {
    setState((previous) => ({ ...previous, batchId, status: 'idle', result: null, formError: null }));
  }, []);

  const setJobDescriptionId = useCallback((jobDescriptionId) => {
    setState((previous) => ({ ...previous, jobDescriptionId, status: 'idle', result: null, formError: null }));
  }, []);

  const setLookupBatchId = useCallback((lookupBatchId) => {
    setState((previous) => ({ ...previous, lookupBatchId, status: 'idle', result: null, formError: null }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const canAnalyze =
    isValidId(state.batchId) && isValidId(state.jobDescriptionId) && state.status !== 'submitting';
  const canLoadRanking = isValidId(state.lookupBatchId) && state.status !== 'submitting';

  const analyze = useCallback(async () => {
    if (!isValidId(state.batchId) || !isValidId(state.jobDescriptionId) || state.status === 'submitting') {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await analyzeScreeningBatch(Number(state.batchId), {
        jobDescriptionId: Number(state.jobDescriptionId),
      });
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.batchId, state.jobDescriptionId, state.status]);

  const loadRanking = useCallback(async () => {
    if (!isValidId(state.lookupBatchId) || state.status === 'submitting') {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await getScreeningBatchRanking(Number(state.lookupBatchId));
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.lookupBatchId, state.status]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    canAnalyze,
    canLoadRanking,
    setBatchId,
    setJobDescriptionId,
    setLookupBatchId,
    analyze,
    loadRanking,
    reset,
  };
}
