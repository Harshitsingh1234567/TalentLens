import { ApiError } from './ApiError';

/**
 * Base URL prepended to every request path.
 *
 * Left empty by default: requests are made to relative paths (e.g.
 * "/api/auth/login"), which the Vite dev server proxies to the backend
 * (see vite.config.js) so the browser sees everything as same-origin.
 * Set VITE_API_BASE_URL to call a backend directly instead - see
 * .env.example for why the empty default exists.
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

let authToken = null;

/**
 * Called by the auth layer whenever the current token changes (login,
 * logout, or session restore) so the client can attach it to subsequent
 * requests. Kept as a plain module-level value rather than reading React
 * context here - this module must stay usable outside of React (and
 * without importing React) so it has no circular dependency on the
 * components that use it.
 */
export function setAuthToken(token) {
  authToken = token;
}

let unauthorizedHandler = null;

/**
 * Registered once by the auth layer. Called only when a request that
 * *was* sent with a token comes back 401 - i.e. the session itself was
 * rejected (expired/invalid), not when a public endpoint like login
 * legitimately rejects bad credentials with its own 401 (see
 * `hadToken` below).
 */
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

async function parseJsonSafely(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function request(path, { method = 'GET', body, signal } = {}) {
  const hadToken = Boolean(authToken);
  const headers = {};
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;

  // A FormData body (used for multipart file uploads, e.g. POST
  // /api/resumes) must NOT be JSON-stringified, and must NOT have an
  // explicit Content-Type set - the browser generates the correct
  // "multipart/form-data; boundary=..." header itself only when it
  // controls serialization. Every other request body is JSON, as
  // before.
  if (body !== undefined && !isFormData) {
    headers['Content-Type'] = 'application/json';
  }
  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
      signal,
    });
  } catch {
    // A network-level failure (backend unreachable, DNS, offline, CORS
    // block, aborted request) never reaches the response-handling below,
    // so it is normalized into the same ApiError shape the rest of the
    // app expects, with a status of 0 to distinguish it from any real
    // HTTP response.
    throw new ApiError({
      status: 0,
      message: 'Unable to reach the server. Check your connection and try again.',
    });
  }

  const data = await parseJsonSafely(response);

  if (!response.ok) {
    const apiError = new ApiError({
      status: response.status,
      message: (data && data.message) || response.statusText || 'Request failed',
      fieldErrors: data && data.fieldErrors,
    });

    if (apiError.isUnauthorized() && hadToken && unauthorizedHandler) {
      unauthorizedHandler();
    }

    throw apiError;
  }

  return data;
}

export const apiClient = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
};
