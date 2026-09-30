/**
 * Thrown by the API client for any non-2xx response.
 *
 * Mirrors the consistent error shape returned by the backend's
 * GlobalExceptionHandler: `{ timestamp, status, error, message,
 * fieldErrors? }`. `fieldErrors` is only present for 400 responses to
 * @Valid request-body validation failures (e.g. register/login); it is
 * undefined otherwise.
 */
export class ApiError extends Error {
  constructor({ status, message, fieldErrors }) {
    super(message || 'Request failed');
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }

  isUnauthorized() {
    return this.status === 401;
  }

  isForbidden() {
    return this.status === 403;
  }

  isNotFound() {
    return this.status === 404;
  }

  isValidationError() {
    return this.status === 400;
  }
}
