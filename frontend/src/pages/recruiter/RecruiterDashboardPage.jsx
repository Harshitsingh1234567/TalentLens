import { Link } from 'react-router-dom';
import { useAuth } from '../../auth/useAuth';
import { Card } from '../../components/Card/Card';
import { Button } from '../../components/Button/Button';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import styles from './RecruiterDashboardPage.module.css';

/**
 * This page links to every recruiter workflow built so far.
 */
export function RecruiterDashboardPage() {
  const { user } = useAuth();

  return (
    <PageContainer title="Recruiter Dashboard" description={`Signed in as ${user.email}`}>
      <div className={styles.grid}>
        <Card>
          <h2>Screening batches</h2>
          <p>Upload multiple resumes at once as a named batch for later candidate screening.</p>
          <Button as={Link} to="/recruiter/screening-batches">
            Manage screening batches
          </Button>
        </Card>

        <Card>
          <h2>Candidate ranking</h2>
          <p>Analyze a screening batch against a job description and see candidates ranked by fit.</p>
          <Button as={Link} to="/recruiter/candidate-ranking">
            Rank candidates
          </Button>
        </Card>
      </div>
    </PageContainer>
  );
}
