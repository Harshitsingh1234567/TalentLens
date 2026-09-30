const STORAGE_KEY = 'resumeAnalyzer.auth';

/**
 * Persisted across browser restarts (localStorage, not sessionStorage)
 * so a page refresh or reopening the tab doesn't force a re-login - the
 * standard expectation for an SPA session. The trade-off is that a JWT
 * in localStorage is readable by any script running on the page (XSS
 * risk), same as sessionStorage; an httpOnly cookie would avoid that,
 * but the backend issues the token in the response body, not a
 * Set-Cookie header, and Phase 7 is frozen, so that isn't an option
 * available to the frontend alone. Only the minimum needed is stored:
 * the token itself plus the two identity fields the UI needs to render
 * role-aware navigation without decoding the token for that purpose -
 * never the password, which the frontend never has after submission.
 */
export function loadStoredSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed.token !== 'string' || !parsed.email || !parsed.role) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function saveStoredSession({ token, email, role }) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, email, role }));
  } catch {
    // A storage failure (private browsing, quota, disabled storage)
    // degrades to "session isn't persisted", not a crash - the user can
    // still use the app for the current page lifetime.
  }
}

export function clearStoredSession() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Nothing further to do if storage itself is unavailable.
  }
}
