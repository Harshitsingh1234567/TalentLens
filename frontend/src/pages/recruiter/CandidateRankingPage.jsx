import { useCandidateRankingWorkflow } from '../../hooks/useCandidateRankingWorkflow';
import { useJobDescriptionSubmit } from '../../hooks/useJobDescriptionSubmit';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { Card } from '../../components/Card/Card';
import { Alert } from '../../components/Alert/Alert';
import { Button } from '../../components/Button/Button';
import { TextField } from '../../components/TextField/TextField';
import { TextAreaField } from '../../components/TextField/TextAreaField';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { SkillTagList } from '../../components/SkillTagList/SkillTagList';
import styles from './CandidateRankingPage.module.css';

function formatPercentage(score) {
  return `${Math.round(score * 100)}%`;
}

function RankingResult({ result }) {
  return (
    <Card className={styles.resultCard}>
      <div className={styles.resultMeta}>
        <span>Batch ID: {result.screeningBatchId}</span>
        <span>Job Description ID: {result.jobDescriptionId}</span>
        <span>Analyzed: {new Date(result.analyzedAt).toLocaleString()}</span>
        <span>Candidates ranked: {result.candidatesRanked}</span>
      </div>

      {result.rankings.length === 0 ? (
        <Alert tone="info">
          This batch has no ranked candidates. It may be empty, or every resume in it may have failed
          to process during upload.
        </Alert>
      ) : (
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">Rank</th>
                <th scope="col">Candidate</th>
                <th scope="col">Final</th>
                <th scope="col">Skills</th>
                <th scope="col">Experience</th>
                <th scope="col">Semantic</th>
                <th scope="col">Matched skills</th>
                <th scope="col">Missing skills</th>
              </tr>
            </thead>
            <tbody>
              {result.rankings.map((candidate) => (
                <tr key={candidate.resumeId}>
                  <td className={styles.rankCell}>#{candidate.rank}</td>
                  <td>{candidate.candidateName || '\u2014'}</td>
                  <td className={styles.scoreCell}>{formatPercentage(candidate.finalScore)}</td>
                  <td className={styles.scoreCell}>{formatPercentage(candidate.skillScore)}</td>
                  <td className={styles.scoreCell}>{formatPercentage(candidate.experienceScore)}</td>
                  <td className={styles.scoreCell}>{formatPercentage(candidate.semanticScore)}</td>
                  <td className={styles.skillsCell}>
                    <SkillTagList skills={candidate.matchedSkills} tone="positive" />
                  </td>
                  <td className={styles.skillsCell}>
                    <SkillTagList skills={candidate.missingSkills} tone="neutral" />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
}

function CreateJobDescriptionHelper() {
  const { content, fieldErrors, formError, status, result, isSubmitting, canSubmit, setContent, submit, reset } =
    useJobDescriptionSubmit();

  async function handleSubmit(event) {
    event.preventDefault();
    await submit();
  }

  if (status === 'success' && result) {
    return (
      <Card className={styles.helperCard}>
        <h2>Need a Job Description ID?</h2>
        <Alert tone="success">
          Job description created. Its ID is <strong>{result.id}</strong> - use it in the form above.
        </Alert>
        <Button variant="secondary" onClick={reset}>
          Create another
        </Button>
      </Card>
    );
  }

  return (
    <Card className={styles.helperCard}>
      <h2>Need a Job Description ID?</h2>
      <p>Submit a job description here to get an ID you can analyze this batch against.</p>
      {formError && <Alert tone="danger">{formError}</Alert>}
      <form onSubmit={handleSubmit} noValidate>
        <TextAreaField
          label="Job description content"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          error={fieldErrors.content}
          disabled={isSubmitting}
          required
        />
        <Button type="submit" isLoading={isSubmitting} disabled={!canSubmit} variant="secondary">
          Submit job description
        </Button>
      </form>
    </Card>
  );
}

export function CandidateRankingPage() {
  const {
    batchId,
    jobDescriptionId,
    lookupBatchId,
    formError,
    status,
    result,
    isSubmitting,
    canAnalyze,
    canLoadRanking,
    setBatchId,
    setJobDescriptionId,
    setLookupBatchId,
    analyze,
    loadRanking,
    reset,
  } = useCandidateRankingWorkflow();

  async function handleAnalyze(event) {
    event.preventDefault();
    await analyze();
  }

  async function handleLoadRanking(event) {
    event.preventDefault();
    await loadRanking();
  }

  return (
    <PageContainer
      title="Candidate ranking"
      description="Analyze a screening batch against a job description, or view its current ranking."
    >
      <div className={styles.formsGrid}>
        <Card>
          <h2>Analyze a screening batch</h2>
          <form onSubmit={handleAnalyze} noValidate>
            <TextField
              label="Batch ID"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              value={batchId}
              onChange={(event) => setBatchId(event.target.value)}
              hint="Shown after you create a screening batch."
              disabled={isSubmitting}
              required
            />
            <TextField
              label="Job Description ID"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              value={jobDescriptionId}
              onChange={(event) => setJobDescriptionId(event.target.value)}
              hint="Must be a job description you submitted."
              disabled={isSubmitting}
              required
            />
            <Button type="submit" isLoading={isSubmitting} disabled={!canAnalyze}>
              Run analysis
            </Button>
          </form>
        </Card>

        <Card>
          <h2>View current ranking</h2>
          <form onSubmit={handleLoadRanking} noValidate>
            <TextField
              label="Batch ID"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              value={lookupBatchId}
              onChange={(event) => setLookupBatchId(event.target.value)}
              hint="The batch must have been analyzed at least once."
              disabled={isSubmitting}
              required
            />
            <Button type="submit" isLoading={isSubmitting} disabled={!canLoadRanking} variant="secondary">
              Load ranking
            </Button>
          </form>
        </Card>
      </div>

      {formError && <Alert tone="danger">{formError}</Alert>}

      {status === 'success' && result && (
        <>
          <RankingResult result={result} />
          <Button onClick={reset} variant="secondary" className={styles.startOver}>
            Start over
          </Button>
        </>
      )}

      {status === 'idle' && !formError && (
        <EmptyState
          title="No ranking loaded yet"
          description="Analyze a screening batch against a job description, or load the current ranking for a batch you've already analyzed."
        />
      )}

      <CreateJobDescriptionHelper />
    </PageContainer>
  );
}
