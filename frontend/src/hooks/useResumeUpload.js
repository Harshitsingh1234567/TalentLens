import { useCallback, useState } from 'react';
import { uploadResume } from '../api/resumeApi';
import { ApiError } from '../api/ApiError';

const ACCEPTED_EXTENSIONS = ['.pdf', '.docx'];
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // Mirrors the backend's servlet.multipart.max-file-size (5MB).

/**
 * A lightweight, non-authoritative check mirroring the backend's actual
 * rules (`ResumeParsingService.detectFileType`, `application.yml`'s
 * max-file-size) purely so obviously-invalid files are rejected
 * instantly, without a round trip. It intentionally mirrors those rules
 * rather than inventing stricter ones, and the backend's own validation
 * (via `ResumeProcessingException` / `MaxUploadSizeExceededException`)
 * remains the actual source of truth - this only ever produces the same
 * class of error the backend would, just faster.
 */
function validateFile(file) {
  const lowerName = file.name.toLowerCase();
  const hasSupportedExtension = ACCEPTED_EXTENSIONS.some((extension) => lowerName.endsWith(extension));
  if (!hasSupportedExtension) {
    return 'Unsupported file type - only PDF and DOCX are supported.';
  }
  if (file.size === 0) {
    return 'The selected file is empty.';
  }
  if (file.size > MAX_FILE_SIZE_BYTES) {
    return 'This file exceeds the 5MB size limit.';
  }
  return null;
}

const INITIAL_STATE = {
  file: null,
  candidateName: '',
  fileError: null,
  formError: null,
  status: 'idle', // 'idle' | 'submitting' | 'success'
  result: null,
};

export function useResumeUpload() {
  const [state, setState] = useState(INITIAL_STATE);

  const selectFile = useCallback((file) => {
    setState((previous) => ({
      ...previous,
      file,
      fileError: validateFile(file),
      formError: null,
      status: 'idle',
      result: null,
    }));
  }, []);

  const clearFile = useCallback(() => {
    setState((previous) => ({ ...previous, file: null, fileError: null }));
  }, []);

  const setCandidateName = useCallback((candidateName) => {
    setState((previous) => ({ ...previous, candidateName }));
  }, []);

  const reset = useCallback(() => setState(INITIAL_STATE), []);

  const submit = useCallback(async () => {
    if (!state.file || state.fileError) {
      return;
    }

    setState((previous) => ({ ...previous, status: 'submitting', formError: null }));

    try {
      const result = await uploadResume({ file: state.file, candidateName: state.candidateName.trim() });
      setState((previous) => ({ ...previous, status: 'success', result }));
    } catch (error) {
      const message =
        error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
      setState((previous) => ({ ...previous, status: 'idle', formError: message }));
    }
  }, [state.file, state.fileError, state.candidateName]);

  return {
    ...state,
    isSubmitting: state.status === 'submitting',
    canSubmit: Boolean(state.file) && !state.fileError && state.status !== 'submitting',
    selectFile,
    clearFile,
    setCandidateName,
    submit,
    reset,
  };
}
