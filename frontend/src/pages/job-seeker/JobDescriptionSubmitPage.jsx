import { Link } from 'react-router-dom';
import { useJobDescriptionSubmit } from '../../hooks/useJobDescriptionSubmit';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { Card } from '../../components/Card/Card';
import { Alert } from '../../components/Alert/Alert';
import { Button } from '../../components/Button/Button';
import { TextAreaField } from '../../components/TextField/TextAreaField';
import styles from './JobDescriptionSubmitPage.module.css';

export function JobDescriptionSubmitPage() {
  const {
    content,
    fieldErrors,
    formError,
    status,
    result,
    isSubmitting,
    canSubmit,
    remainingCharacters,
    maxContentLength,
    setContent,
    submit,
    reset,
  } = useJobDescriptionSubmit();

  async function handleSubmit(event) {
    event.preventDefault();
    await submit();
  }

  if (status === 'success' && result) {
    return (
      <PageContainer title="Submit job description">
        <Card>
          <Alert tone="success">Your job description was submitted successfully.</Alert>
          <dl className={styles.summaryList}>
            <div>
              <dt>Job description ID</dt>
              <dd>{result.id}</dd>
            </div>
            <div>
              <dt>Submitted</dt>
              <dd>{new Date(result.submittedAt).toLocaleString()}</dd>
            </div>
          </dl>
          <div className={styles.actions}>
            <Button onClick={reset}>Submit another job description</Button>
            <Button as={Link} to="/job-seeker/dashboard" variant="secondary">
              Back to dashboard
            </Button>
          </div>
        </Card>
      </PageContainer>
    );
  }

  const characterCountHint =
    remainingCharacters >= 0
      ? `${content.length} / ${maxContentLength} characters`
      : `${Math.abs(remainingCharacters)} characters over the ${maxContentLength} limit`;

  return (
    <PageContainer
      title="Submit job description"
      description="Paste a job posting so it's on file for matching and analysis."
    >
      <Card>
        {formError && <Alert tone="danger">{formError}</Alert>}

        <form onSubmit={handleSubmit} noValidate>
          <TextAreaField
            label="Job description content"
            name="content"
            value={content}
            onChange={(event) => setContent(event.target.value)}
            error={fieldErrors.content}
            hint={!fieldErrors.content ? characterCountHint : undefined}
            disabled={isSubmitting}
            required
          />

          <Button type="submit" isLoading={isSubmitting} disabled={!canSubmit} className={styles.submit}>
            Submit job description
          </Button>
        </form>
      </Card>
    </PageContainer>
  );
}
