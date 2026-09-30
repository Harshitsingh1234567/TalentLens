import { Link } from 'react-router-dom';
import { EmptyState } from '../components/EmptyState/EmptyState';
import { Button } from '../components/Button/Button';
import { PageContainer } from '../components/PageContainer/PageContainer';

export function NotFoundPage() {
  return (
    <PageContainer title="Page not found">
      <EmptyState
        title="We couldn't find that page"
        description="The page you're looking for doesn't exist or may have moved."
        action={
          <Link to="/">
            <Button>Go to home</Button>
          </Link>
        }
      />
    </PageContainer>
  );
}
