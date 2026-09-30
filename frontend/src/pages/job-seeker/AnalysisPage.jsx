import { Link } from 'react-router-dom';
import { useAnalysisWorkflow } from '../../hooks/useAnalysisWorkflow';
import { useInterviewQuestionsLookup } from '../../hooks/useInterviewQuestionsLookup';
import { useMatchSelectorOptions } from '../../hooks/useMatchSelectorOptions';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { Card } from '../../components/Card/Card';
import { Alert } from '../../components/Alert/Alert';
import { Badge } from '../../components/Badge/Badge';
import { Button } from '../../components/Button/Button';
import { TextField } from '../../components/TextField/TextField';
import { SelectField } from '../../components/TextField/SelectField';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { Spinner } from '../../components/Spinner/Spinner';
import { ScoreRing } from '../../components/ScoreRing/ScoreRing';
import { ScoreBar } from '../../components/ScoreBar/ScoreBar';
import { SkillTagList } from '../../components/SkillTagList/SkillTagList';
import { BulletList } from '../../components/BulletList/BulletList';
import { AnalysisIcon, SparkleIcon, UploadIcon, DocumentIcon } from '../../components/Icon/Icon';
import styles from './AnalysisPage.module.css';

const AI_STATUS_MESSAGE = {
  FALLBACK: 'AI enhancement was unavailable for this analysis - showing standard results only.',
  DISABLED: 'AI enhancement is not enabled - showing standard results only.',
};

function AnalysisResult({ result }) {
  const isAiEnhanced = result.aiEnhancementStatus === 'SUCCESS';
  const fallbackMessage = AI_STATUS_MESSAGE[result.aiEnhancementStatus];

  return (
    <Card className={styles.resultCard}>
      <div className={styles.resultMeta}>
        <span className={styles.metaItem}>
          Analysis <strong className={styles.tabularNums}>#{result.id}</strong>
        </span>
        <span className={styles.metaItem}>
          Resume <strong className={styles.tabularNums}>#{result.resumeId}</strong>
        </span>
        <span className={styles.metaItem}>
          Job description <strong className={styles.tabularNums}>#{result.jobDescriptionId}</strong>
        </span>
        <span className={styles.metaItem}>{new Date(result.createdAt).toLocaleString()}</span>
      </div>

      <div className={styles.scoreHeader}>
        <ScoreRing score={result.finalScore} label="Overall match" />
        <div className={styles.componentScores}>
          <ScoreBar label="Skills" score={result.skillScore} />
          <ScoreBar label="Experience" score={result.experienceScore} />
          <ScoreBar label="Semantic fit" score={result.semanticScore} />
        </div>
      </div>

      <p className={styles.explanation}>{result.scoreExplanation}</p>

      <div className={styles.skillsGrid}>
        <section aria-labelledby="matched-skills-heading">
          <h3 id="matched-skills-heading">Matched skills</h3>
          <SkillTagList
            skills={result.matchedSkills}
            tone="positive"
            emptyLabel="No matching skills were found."
          />
        </section>
        <section aria-labelledby="missing-skills-heading">
          <h3 id="missing-skills-heading">Missing skills</h3>
          <SkillTagList
            skills={result.missingSkills}
            tone="neutral"
            emptyLabel="No missing skills were identified."
          />
        </section>
      </div>

      {fallbackMessage && <Alert tone="warning">{fallbackMessage}</Alert>}

      <div className={styles.insightsHeader}>
        <h3 className={styles.insightsHeading}>Detailed insights</h3>
        {isAiEnhanced && (
          <Badge tone="insight" icon={<SparkleIcon size={13} />}>
            AI-enhanced
          </Badge>
        )}
      </div>

      {isAiEnhanced ? (
        <>
          {result.aiAdvancedAtsIssues.length > 0 && (
            <BulletList title="ATS issues" items={result.aiAdvancedAtsIssues} />
          )}
          {result.aiFormattingInsights.length > 0 && (
            <BulletList title="Formatting insights" items={result.aiFormattingInsights} />
          )}
          {result.aiContentSuggestions.length > 0 && (
            <BulletList title="Content suggestions" items={result.aiContentSuggestions} />
          )}
          {result.aiInterviewQuestions.length > 0 && (
            <BulletList title="Interview questions" items={result.aiInterviewQuestions} />
          )}
        </>
      ) : (
        <>
          <BulletList
            title="ATS issues"
            items={result.atsIssues}
            emptyLabel="No ATS issues were found."
          />
          <BulletList
            title="Formatting & readability"
            items={result.formattingIssues}
            emptyLabel="No formatting issues were found."
          />
          <BulletList
            title="Content suggestions"
            items={result.contentSuggestions}
            emptyLabel="No content suggestions were generated."
          />
          <BulletList
            title="Interview questions"
            items={result.interviewQuestions}
            emptyLabel="No interview questions were generated."
          />
        </>
      )}
    </Card>
  );
}

export function AnalysisPage() {
  const {
    resumeId,
    jobDescriptionId,
    lookupAnalysisId,
    formError,
    status,
    result,
    isSubmitting,
    canRunAnalysis,
    canLoadAnalysis,
    setResumeId,
    setJobDescriptionId,
    setLookupAnalysisId,
    runAnalysis,
    loadAnalysis,
    reset,
  } = useAnalysisWorkflow();

  const questionsLookup = useInterviewQuestionsLookup();

  const {
    status: selectorStatus,
    resumes,
    jobDescriptions,
    error: selectorError,
    reload: reloadSelectorOptions,
  } = useMatchSelectorOptions();

  async function handleRunAnalysis(event) {
    event.preventDefault();
    await runAnalysis();
  }

  async function handleLoadAnalysis(event) {
    event.preventDefault();
    await loadAnalysis();
  }

  async function handleLookupQuestions(event) {
    event.preventDefault();
    await questionsLookup.submit();
  }

  const resumeOptions = [
    { value: '', label: 'Select a resume\u2026' },
    ...resumes.map((resume) => ({ value: String(resume.id), label: resume.originalFilename })),
  ];
  const jobDescriptionOptions = [
    { value: '', label: 'Select a job description\u2026' },
    ...jobDescriptions.map((jobDescription) => ({
      value: String(jobDescription.id),
      label: jobDescription.contentPreview,
    })),
  ];

  return (
    <PageContainer
      title="Resume analysis"
      description="Run a full ATS and match analysis, or revisit one you've already run."
    >
      <div className={styles.formsGrid}>
        <Card>
          <h2>Run a new analysis</h2>

          {selectorStatus === 'loading' && (
            <Spinner label="Loading your resumes and job descriptions\u2026" />
          )}

          {selectorStatus === 'error' && (
            <>
              <Alert tone="danger">{selectorError}</Alert>
              <Button type="button" variant="secondary" onClick={reloadSelectorOptions}>
                Try again
              </Button>
            </>
          )}

          {selectorStatus === 'success' && (resumes.length === 0 || jobDescriptions.length === 0) && (
            <div className={styles.selectorEmptyStates}>
              {resumes.length === 0 && (
                <EmptyState
                  icon={<UploadIcon size={20} />}
                  title="No resumes yet"
                  description="Upload a resume before you can run an analysis."
                  action={
                    <Button as={Link} to="/job-seeker/resumes/upload">
                      Upload resume
                    </Button>
                  }
                />
              )}
              {jobDescriptions.length === 0 && (
                <EmptyState
                  icon={<DocumentIcon size={20} />}
                  title="No job descriptions yet"
                  description="Submit a job description before you can run an analysis."
                  action={
                    <Button as={Link} to="/job-seeker/job-descriptions/new">
                      Submit job description
                    </Button>
                  }
                />
              )}
            </div>
          )}

          {selectorStatus === 'success' && resumes.length > 0 && jobDescriptions.length > 0 && (
            <form onSubmit={handleRunAnalysis} noValidate>
              <SelectField
                label="Resume"
                value={resumeId}
                onChange={(event) => setResumeId(event.target.value)}
                options={resumeOptions}
                disabled={isSubmitting}
                required
              />
              <SelectField
                label="Job description"
                value={jobDescriptionId}
                onChange={(event) => setJobDescriptionId(event.target.value)}
                options={jobDescriptionOptions}
                disabled={isSubmitting}
                required
              />
              <Button type="submit" isLoading={isSubmitting} disabled={!canRunAnalysis}>
                Run analysis
              </Button>
            </form>
          )}
        </Card>

        <Card>
          <h2>View an existing analysis</h2>
          <form onSubmit={handleLoadAnalysis} noValidate>
            <TextField
              label="Analysis ID"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              value={lookupAnalysisId}
              onChange={(event) => setLookupAnalysisId(event.target.value)}
              hint="Shown after you run an analysis."
              disabled={isSubmitting}
              required
            />
            <Button type="submit" isLoading={isSubmitting} disabled={!canLoadAnalysis} variant="secondary">
              Load analysis
            </Button>
          </form>
        </Card>
      </div>

      {formError && <Alert tone="danger">{formError}</Alert>}

      {status === 'success' && result && (
        <>
          <AnalysisResult result={result} />
          <Button onClick={reset} variant="secondary" className={styles.runAnother}>
            Start over
          </Button>
        </>
      )}

      {status === 'idle' && !formError && (
        <EmptyState
          icon={<AnalysisIcon size={20} />}
          title="No analysis loaded yet"
          description="Run a new analysis or load an existing one by its ID to see the full breakdown."
        />
      )}

      <Card className={styles.questionsCard}>
        <h2>Look up interview questions</h2>
        <p>Fetch just the interview questions for an analysis you've already run.</p>
        <form onSubmit={handleLookupQuestions} noValidate>
          <TextField
            label="Analysis ID"
            type="number"
            inputMode="numeric"
            min="1"
            step="1"
            value={questionsLookup.analysisId}
            onChange={(event) => questionsLookup.setAnalysisId(event.target.value)}
            disabled={questionsLookup.isSubmitting}
            required
          />
          <Button
            type="submit"
            isLoading={questionsLookup.isSubmitting}
            disabled={!questionsLookup.canSubmit}
            variant="secondary"
          >
            Get interview questions
          </Button>
        </form>

        {questionsLookup.formError && <Alert tone="danger">{questionsLookup.formError}</Alert>}

        {questionsLookup.status === 'success' && questionsLookup.result && (
          <div className={styles.questionsResult}>
            <BulletList
              title="Interview questions"
              items={questionsLookup.result.questions}
              emptyLabel="No interview questions were generated."
            />
            {questionsLookup.result.aiQuestions.length > 0 && (
              <BulletList title="AI-enhanced interview questions" items={questionsLookup.result.aiQuestions} />
            )}
          </div>
        )}
      </Card>
    </PageContainer>
  );
}
