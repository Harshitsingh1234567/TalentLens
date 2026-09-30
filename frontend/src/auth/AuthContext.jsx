import { useCallback, useEffect, useMemo, useState } from 'react';
import { login as loginRequest, register as registerRequest } from '../api/authApi';
import { setAuthToken, setUnauthorizedHandler } from '../api/client';
import { clearStoredSession, loadStoredSession, saveStoredSession } from './authStorage';
import { AuthContext } from './authContextInstance';
import { isJwtExpired } from './jwt';

function toSession(authResponse) {
  return {
    token: authResponse.token,
    email: authResponse.email,
    role: authResponse.role,
  };
}

/**
 * Restoring a previous session is a synchronous read of localStorage (no
 * server round-trip is needed - the token already carries everything the
 * UI requires), so it happens directly in `useState`'s lazy initializer
 * rather than in a post-mount effect. An expired leftover token is
 * treated the same as no session at all and cleared, rather than being
 * sent to the backend just to be rejected.
 */
function restoreSession() {
  const stored = loadStoredSession();
  if (stored && !isJwtExpired(stored.token)) {
    setAuthToken(stored.token);
    return stored;
  }
  if (stored) {
    clearStoredSession();
  }
  return null;
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(restoreSession);

  const applySession = useCallback((nextSession) => {
    setSession(nextSession);
    setAuthToken(nextSession ? nextSession.token : null);
    if (nextSession) {
      saveStoredSession(nextSession);
    } else {
      clearStoredSession();
    }
  }, []);

  // The API client has no React context of its own; it calls this
  // handler for any request that carried a token and still came back
  // 401, meaning the session itself (not just one login attempt) was
  // rejected - expired, revoked, or otherwise invalid. Clearing session
  // state here is what lets ProtectedRoute redirect to /login. This is a
  // genuine synchronization with an external system (the module-level
  // API client), which is exactly what an effect is for.
  useEffect(() => {
    setUnauthorizedHandler(() => applySession(null));
    return () => setUnauthorizedHandler(null);
  }, [applySession]);

  const login = useCallback(
    async (email, password) => {
      const response = await loginRequest({ email, password });
      applySession(toSession(response));
      return response;
    },
    [applySession],
  );

  const register = useCallback((email, password, role) => registerRequest({ email, password, role }), []);

  const logout = useCallback(() => {
    applySession(null);
  }, [applySession]);

  const value = useMemo(
    () => ({
      user: session ? { email: session.email, role: session.role } : null,
      isAuthenticated: Boolean(session),
      login,
      register,
      logout,
    }),
    [session, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
