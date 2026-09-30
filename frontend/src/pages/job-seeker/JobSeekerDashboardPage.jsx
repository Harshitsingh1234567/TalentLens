import { useAuth } from '../../auth/useAuth';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { QuickActionCard } from '../../components/QuickActionCard/QuickActionCard';
import { StepFlow } from '../../components/StepFlow/StepFlow';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { UploadIcon, DocumentIcon, MatchIcon, AnalysisIcon, ImproveIcon, SparkleIcon } from '../../components/Icon/Icon';
import styles from './JobSeekerDashboardPage.module.css';

const WORKFLOW_STEPS = [
  {
    title: 'Upload resume',
    description: 'PDF or DOCX',
    icon: <UploadIcon size={18} />,
    to: '/job-seeker/resumes/upload',
  },
  {
    title: 'Job description',
    description: 'Paste the posting',
    icon: <DocumentIcon size={18} />,
    to: '/job-seeker/job-descriptions/new',
  },
  {
    title: 'Match',
    description: 'See the score',
    icon: <MatchIcon size={18} />,
    to: '/job-seeker/matches/new',
  },
  {
    title: 'Analyze',
    description: 'Full ATS breakdown',
    icon: <AnalysisIcon size={18} />,
    to: '/job-seeker/analysis',
  },
  {
    title: 'Improve',
    description: 'Act on the suggestions',
    icon: <ImproveIcon size={18} />,
  },
];

const QUICK_ACTIONS = [
  {
    icon: <UploadIcon size={20} />,
    title: 'Upload resume',
    description: "Add a PDF or DOCX resume so it's on file for matching and analysis.",
    to: '/job-seeker/resumes/upload',
    cta: 'Upload resume',
  },
  {
    icon: <DocumentIcon size={20} />,
    title: 'Submit job description',
    description: "Paste a job posting so it's on file for matching and analysis.",
    to: '/job-seeker/job-descriptions/new',
    cta: 'Submit job description',
  },
  {
    icon: <MatchIcon size={20} />,
    title: 'Run a match',
    description: 'See how well an uploaded resume aligns with a submitted job description.',
    to: '/job-seeker/matches/new',
    cta: 'Run match',
  },
  {
    icon: <AnalysisIcon size={20} />,
    title: 'Resume analysis',
    description: 'Get a full ATS, formatting, and content analysis with interview questions.',
    to: '/job-seeker/analysis',
    cta: 'Run analysis',
  },
];

/**
 * This page links to every document-workflow, matching, and analysis
 * page built so far. The five-step flow, quick actions, and page
 * structure are static guidance, not a progress tracker - this app
 * doesn't persist any cross-page "resume uploaded / match run" state
 * anywhere (each workflow's state lives only in that page's own hook
 * while it's mounted), so nothing here claims to know what the person
 * has already done.
 */
export function JobSeekerDashboardPage() {
  const { user } = useAuth();
  useDocumentTitle('Job Seeker Dashboard');

  return (
    <PageContainer>
      <section className={styles.hero}>
        <span className={styles.eyebrow}>
          <span className={styles.eyebrowDot} aria-hidden="true" />
          Signed in as {user.email}
        </span>
        <h1 className={styles.heroTitle}>Turn your resume into a stronger application</h1>
        <p className={styles.heroDescription}>
          Upload a resume and a job description, then match and analyze them together to see exactly what's
          working, what's missing, and what to fix before you apply.
        </p>
      </section>

      <section className={styles.workflowSection} aria-labelledby="workflow-heading">
        <h2 id="workflow-heading" className={styles.sectionHeading}>
          Your workflow
        </h2>
        <div className={styles.workflowCard}>
          <StepFlow steps={WORKFLOW_STEPS} />
        </div>
      </section>

      <div className={styles.mainGrid}>
        <section aria-labelledby="quick-actions-heading">
          <h2 id="quick-actions-heading" className={styles.sectionHeading}>
            Quick actions
          </h2>
          <div className={styles.actionsGrid}>
            {QUICK_ACTIONS.map((action) => (
              <QuickActionCard key={action.to} {...action} />
            ))}
          </div>
        </section>

        <section aria-labelledby="activity-heading" className={styles.activityColumn}>
          <h2 id="activity-heading" className={styles.sectionHeading}>
            Recent activity
          </h2>
          <EmptyState
            icon={<SparkleIcon size={20} />}
            title="Nothing here yet"
            description="Your recent uploads, matches, and analyses will show up here once you start working through the steps above."
          />
        </section>
      </div>
    </PageContainer>
  );
}
