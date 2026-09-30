import { useCallback, useState } from 'react';
import { getInterviewQuestions } from '../api/analysisApi';
import { ApiError } from '../api/ApiError';

function isValidId(value) {
  return /^\d+$/.test(value) && Number(value) > 0;
}

const INITIAL_STATE = {
  analysisId: '',
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

/**
 * A narrower lookup than `useAnalysisWorkflow`: fetches only the
 * interview questions for a known analysis id via the dedicated
 * GET /api/analysis/{id}/interview-questions endpoint, without loading
 * (or re-displaying) the full score/skills/issues breakdown.
 */
export function useInterviewQuestionsLookup() {
  const [state, setState] = useState(INITIAL_STATE);

  const setAnalysisId = useCallback((analysisId) => {
    setState((previous) => ({ ...previous, analysisId, status: 'idle', result: null, formError: null }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const canSubmit = isValidId(state.analysisId) && state.status !== 'submitting';

  const submit = useCallback(async () => {
    if (!isValidId(state.analysisId) || state.status === 'submitting') {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await getInterviewQuestions(Number(state.analysisId));
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.analysisId, state.status]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    canSubmit,
    setAnalysisId,
    submit,
    reset,
  };
}
