import { useCallback, useState } from 'react';
import { submitJobDescription } from '../api/jobDescriptionApi';
import { ApiError } from '../api/ApiError';

const MAX_CONTENT_LENGTH = 20_000; // Mirrors JobDescriptionRequest's @Size(max = 20_000).

const INITIAL_STATE = {
  content: '',
  fieldErrors: {},
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

export function useJobDescriptionSubmit() {
  const [state, setState] = useState(INITIAL_STATE);

  const setContent = useCallback((content) => {
    setState((previous) => ({ ...previous, content, fieldErrors: {}, status: 'idle', result: null }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const submit = useCallback(async () => {
    if (!state.content.trim() || state.content.length > MAX_CONTENT_LENGTH || state.status === 'submitting') {
      return;
    }

    setState((previous) => ({ ...previous, status: 'submitting', formError: null, fieldErrors: {} }));

    try {
      const result = await submitJobDescription({ content: state.content.trim() });
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      if (error instanceof ApiError && error.isValidationError() && error.fieldErrors) {
        setState((previous) => ({ ...previous, status: 'idle', fieldErrors: error.fieldErrors }));
      } else {
        const message =
          error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
        setState((previous) => ({ ...previous, status: 'idle', formError: message }));
      }
    }
  }, [state.content, state.status]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    canSubmit:
      Boolean(state.content.trim()) &&
      state.content.length <= MAX_CONTENT_LENGTH &&
      state.status !== 'submitting',
    remainingCharacters: MAX_CONTENT_LENGTH - state.content.length,
    maxContentLength: MAX_CONTENT_LENGTH,
    setContent,
    submit,
    reset,
  };
}
