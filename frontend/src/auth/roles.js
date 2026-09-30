/**
 * Mirrors `com.resumeanalyzer.entity.Role` exactly.
 *
 * The backend serializes this as the bare enum name - "JOB_SEEKER" /
 * "RECRUITER" - in both AuthResponse.role and UserResponse.role, with no
 * "ROLE_" prefix. (Spring Security itself prefixes granted authorities
 * with "ROLE_" internally for its own hasRole()/@PreAuthorize checks, but
 * that is a server-side implementation detail; it never appears in the
 * actual JSON the API returns, so the frontend must not expect it.)
 */
export const ROLES = Object.freeze({
  JOB_SEEKER: 'JOB_SEEKER',
  RECRUITER: 'RECRUITER',
});

export const ROLE_OPTIONS = [
  { value: ROLES.JOB_SEEKER, label: 'Job Seeker' },
  { value: ROLES.RECRUITER, label: 'Recruiter' },
];

/**
 * Where each role lands after login, and the base path their protected
 * pages live under - the single source of truth both `RoleRoute` and any
 * post-login redirect use, so they can never drift out of sync.
 */
export const ROLE_HOME_PATH = Object.freeze({
  [ROLES.JOB_SEEKER]: '/job-seeker/dashboard',
  [ROLES.RECRUITER]: '/recruiter/dashboard',
});
