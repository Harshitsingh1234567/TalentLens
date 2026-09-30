import { useCallback, useState } from 'react';
import { matchResumeToJobDescription } from '../api/matchApi';
import { ApiError } from '../api/ApiError';

/**
 * A positive-integer check mirroring the one real constraint the backend
 * places on these fields (`MatchRequest`'s `@NotNull` on both ids - there
 * is no format constraint beyond "a real id"). This only prevents an
 * obviously-unusable request (blank, negative, non-numeric) from being
 * sent; whether an id actually exists and is owned by the caller is, and
 * can only be, decided by the backend (`MatchService`), which returns a
 * 404 either way without distinguishing "doesn't exist" from "not
 * yours".
 */
function isValidId(value) {
  return /^\d+$/.test(value) && Number(value) > 0;
}

const INITIAL_STATE = {
  resumeId: '',
  jobDescriptionId: '',
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

export function useMatchResumeToJob() {
  const [state, setState] = useState(INITIAL_STATE);

  const setResumeId = useCallback((resumeId) => {
    setState((previous) => ({ ...previous, resumeId, status: 'idle', result: null, formError: null }));
  }, []);

  const setJobDescriptionId = useCallback((jobDescriptionId) => {
    setState((previous) => ({ ...previous, jobDescriptionId, status: 'idle', result: null, formError: null }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const canSubmit =
    isValidId(state.resumeId) && isValidId(state.jobDescriptionId) && state.status !== 'submitting';

  const submit = useCallback(async () => {
    if (!isValidId(state.resumeId) || !isValidId(state.jobDescriptionId) || state.status === 'submitting') {
      return;
    }

    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));

    try {
      const result = await matchResumeToJobDescription({
        resumeId: Number(state.resumeId),
        jobDescriptionId: Number(state.jobDescriptionId),
      });
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      // A 404 here already comes back as one of two specific, accurate
      // messages from MatchService ("No resume found for the given id" /
      // "No job description found for the given id") - shown as-is
      // rather than re-interpreted, since re-wording it risks being less
      // accurate than what the backend already determined.
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.resumeId, state.jobDescriptionId, state.status]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    canSubmit,
    setResumeId,
    setJobDescriptionId,
    submit,
    reset,
  };
}
