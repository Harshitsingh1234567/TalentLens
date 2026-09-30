import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

/**
 * Nested inside `ProtectedRoute`, so `user` is always present here - this
 * only decides whether the *authenticated* user's role matches what this
 * branch of the route tree requires. A mismatch (e.g. a Job Seeker
 * trying a Recruiter URL directly) renders /forbidden rather than
 * silently redirecting to their own dashboard, so the user gets clear
 * feedback about *why* the page they asked for didn't load.
 */
export function RoleRoute({ allowedRole }) {
  const { user } = useAuth();

  if (!user || user.role !== allowedRole) {
    return <Navigate to="/forbidden" replace />;
  }

  return <Outlet />;
}
