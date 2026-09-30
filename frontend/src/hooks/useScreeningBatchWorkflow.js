import { useCallback, useState } from 'react';
import { createScreeningBatch, getScreeningBatch } from '../api/screeningBatchApi';
import { ApiError } from '../api/ApiError';

const MAX_FILES_PER_BATCH = 50; // Mirrors ScreeningBatchService.MAX_FILES_PER_BATCH.
const MAX_NAME_LENGTH = 255; // Mirrors the batch name column/validation limit.

function isValidId(value) {
  return /^\d+$/.test(value) && Number(value) > 0;
}

const INITIAL_STATE = {
  name: '',
  files: [],
  lookupBatchId: '',
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

/**
 * Backs both ways of getting a `ScreeningBatchResponse` onto the page -
 * creating a new batch (`POST /api/screening-batches`) and loading a
 * previously created one (`GET /api/screening-batches/{id}`) - since
 * they return the same shape and the page displays either result
 * identically. Starting one action clears any result from the other.
 *
 * Only the two request-level constraints the backend actually enforces
 * outright (blank/oversized name, zero or too many files - see
 * `ScreeningBatchService.createBatch`) are checked client-side, to avoid
 * a guaranteed-400 round trip. Whether any individual file parses is
 * deliberately NOT pre-checked here - that is exactly the partial-success
 * outcome the backend is designed to report per file, and duplicating
 * that check client-side would only get in the way of seeing it.
 */
export function useScreeningBatchWorkflow() {
  const [state, setState] = useState(INITIAL_STATE);

  const setName = useCallback((name) => {
    setState((previous) => ({ ...previous, name, status: 'idle', result: null, formError: null }));
  }, []);

  const addFiles = useCallback((newFiles) => {
    setState((previous) => ({
      ...previous,
      files: [...previous.files, ...newFiles],
      status: 'idle',
      result: null,
      formError: null,
    }));
  }, []);

  const removeFile = useCallback((index) => {
    setState((previous) => ({
      ...previous,
      files: previous.files.filter((_, fileIndex) => fileIndex !== index),
    }));
  }, []);

  const setLookupBatchId = useCallback((lookupBatchId) => {
    setState((previous) => ({ ...previous, lookupBatchId, status: 'idle', result: null, formError: null }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const trimmedName = state.name.trim();
  const nameError = trimmedName.length > MAX_NAME_LENGTH ? `Batch name must not exceed ${MAX_NAME_LENGTH} characters.` : null;

  const canCreateBatch =
    trimmedName.length > 0 &&
    trimmedName.length <= MAX_NAME_LENGTH &&
    state.files.length > 0 &&
    state.files.length <= MAX_FILES_PER_BATCH &&
    state.status !== 'submitting';

  const canLoadBatch = isValidId(state.lookupBatchId) && state.status !== 'submitting';

  const createBatch = useCallback(async () => {
    if (!canCreateBatch) {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await createScreeningBatch({ name: trimmedName, files: state.files });
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [canCreateBatch, trimmedName, state.files]);

  const loadBatch = useCallback(async () => {
    if (!canLoadBatch) {
      return;
    }
    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));
    try {
      const result = await getScreeningBatch(Number(state.lookupBatchId));
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [canLoadBatch, state.lookupBatchId]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    nameError,
    maxFilesPerBatch: MAX_FILES_PER_BATCH,
    canCreateBatch,
    canLoadBatch,
    setName,
    addFiles,
    removeFile,
    setLookupBatchId,
    createBatch,
    loadBatch,
    reset,
  };
}
