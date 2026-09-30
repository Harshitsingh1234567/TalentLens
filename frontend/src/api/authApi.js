import { apiClient } from './client';

/**
 * Wraps POST /api/auth/register exactly as the backend defines it:
 * request `{ email, password, role }`, response `{ id, email, role }`
 * (no token - the caller must still log in separately, matching the
 * backend's actual contract).
 */
export function register({ email, password, role }) {
  return apiClient.post('/api/auth/register', { email, password, role });
}

/**
 * Wraps POST /api/auth/login exactly as the backend defines it: request
 * `{ email, password }`, response `{ token, tokenType, email, role }`.
 */
export function login({ email, password }) {
  return apiClient.post('/api/auth/login', { email, password });
}
