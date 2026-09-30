import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLES, ROLE_HOME_PATH } from '../auth/roles';
import { AppShell } from '../layouts/AppShell';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ForbiddenPage } from '../pages/ForbiddenPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { JobSeekerDashboardPage } from '../pages/job-seeker/JobSeekerDashboardPage';
import { ResumeUploadPage } from '../pages/job-seeker/ResumeUploadPage';
import { JobDescriptionSubmitPage } from '../pages/job-seeker/JobDescriptionSubmitPage';
import { RunMatchPage } from '../pages/job-seeker/RunMatchPage';
import { AnalysisPage } from '../pages/job-seeker/AnalysisPage';
import { RecruiterDashboardPage } from '../pages/recruiter/RecruiterDashboardPage';
import { ScreeningBatchPage } from '../pages/recruiter/ScreeningBatchPage';
import { CandidateRankingPage } from '../pages/recruiter/CandidateRankingPage';
import { ProtectedRoute } from './ProtectedRoute';
import { RoleRoute } from './RoleRoute';

/**
 * "/" has no content of its own - it only ever sends the visitor
 * somewhere meaningful: their own dashboard if already signed in, or
 * /login otherwise.
 */
function RootRedirect() {
  const { isAuthenticated, user } = useAuth();

  return <Navigate to={isAuthenticated ? ROLE_HOME_PATH[user.role] : '/login'} replace />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route path="/forbidden" element={<ForbiddenPage />} />

          <Route element={<RoleRoute allowedRole={ROLES.JOB_SEEKER} />}>
            <Route path="/job-seeker/dashboard" element={<JobSeekerDashboardPage />} />
            <Route path="/job-seeker/resumes/upload" element={<ResumeUploadPage />} />
            <Route path="/job-seeker/job-descriptions/new" element={<JobDescriptionSubmitPage />} />
            <Route path="/job-seeker/matches/new" element={<RunMatchPage />} />
            <Route path="/job-seeker/analysis" element={<AnalysisPage />} />
          </Route>

          <Route element={<RoleRoute allowedRole={ROLES.RECRUITER} />}>
            <Route path="/recruiter/dashboard" element={<RecruiterDashboardPage />} />
            <Route path="/recruiter/screening-batches" element={<ScreeningBatchPage />} />
            <Route path="/recruiter/candidate-ranking" element={<CandidateRankingPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
