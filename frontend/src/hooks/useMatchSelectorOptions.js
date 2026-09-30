import { useCallback, useEffect, useState } from 'react';
import { listResumes } from '../api/resumeApi';
import { listJobDescriptions } from '../api/jobDescriptionApi';
import { ApiError } from '../api/ApiError';

const INITIAL_STATE = {
  status: 'loading', // 'loading' | 'success' | 'error'
  resumes: [],
  jobDescriptions: [],
  error: null,
};

/**
 * Loads GET /api/resumes and GET /api/job-descriptions together (both
 * are needed before the matching form is usable), so a job seeker can
 * pick a resume and a job description by name instead of typing their
 * numeric ids. Both lists are already scoped to the authenticated
 * caller by the backend; this hook does no additional filtering.
 *
 * `reload()` is exposed for the error state's retry action, and re-runs
 * both requests from scratch.
 */
export function useMatchSelectorOptions() {
  const [state, setState] = useState(INITIAL_STATE);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let isActive = true;

    Promise.all([listResumes(), listJobDescriptions()])
      .then(([resumes, jobDescriptions]) => {
        if (isActive) {
          setState({ status: 'success', resumes, jobDescriptions, error: null });
        }
      })
      .catch((error) => {
        if (!isActive) {
          return;
        }
        const message =
          error instanceof ApiError ? error.message : 'Something went wrong. Please try again.';
        setState({ status: 'error', resumes: [], jobDescriptions: [], error: message });
      });

    return () => {
      isActive = false;
    };
  }, [reloadToken]);

  // Resetting to the 'loading' state happens here, synchronously in the
  // same event handler that triggers the refetch, rather than at the top
  // of the effect above - the effect only needs to synchronize with the
  // fetch itself. On first mount, INITIAL_STATE already starts as
  // 'loading', so no reset is needed there either.
  const reload = useCallback(() => {
    setState((previous) => ({ ...previous, status: 'loading', error: null }));
    setReloadToken((token) => token + 1);
  }, []);

  return { ...state, reload };
}
