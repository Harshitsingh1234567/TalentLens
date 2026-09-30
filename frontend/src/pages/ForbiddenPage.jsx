import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLE_HOME_PATH } from '../auth/roles';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { Button } from '../components/Button/Button';
import { PageContainer } from '../components/PageContainer/PageContainer';

export function ForbiddenPage() {
  const { user } = useAuth();
  const homePath = user ? ROLE_HOME_PATH[user.role] : '/login';

  return (
    <PageContainer title="Access denied">
      <EmptyState
        title="You don't have access to that page"
        description="This section isn't available for your account type."
        action={
          <Link to={homePath}>
            <Button>Go to my dashboard</Button>
          </Link>
        }
      />
    </PageContainer>
  );
}
