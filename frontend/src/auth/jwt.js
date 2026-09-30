/**
 * Decodes the payload of a JWT without verifying its signature.
 *
 * This is intentionally NOT authentication or verification - only the
 * backend can verify a token's signature, and it already does so on
 * every request (`JwtService`/`JwtAuthenticationFilter`). This is used
 * purely so the UI can proactively notice an obviously-expired token
 * (e.g. one left over from a previous day) and avoid firing a request
 * that the backend would just reject anyway. The backend's own 401
 * response remains the definitive, authoritative expiry/validity check.
 *
 * Returns null for any malformed token instead of throwing, so callers
 * can treat "can't read it" the same as "not usable".
 */
export function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') {
    return null;
  }

  const parts = token.split('.');
  if (parts.length !== 3) {
    return null;
  }

  try {
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const json = atob(padded);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/**
 * True if the token's "exp" claim (seconds since epoch, set by the
 * backend's JwtService) is missing, unreadable, or in the past.
 */
export function isJwtExpired(token) {
  const payload = decodeJwtPayload(token);
  if (!payload || typeof payload.exp !== 'number') {
    return true;
  }
  return payload.exp * 1000 <= Date.now();
}
