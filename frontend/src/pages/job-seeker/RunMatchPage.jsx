import { Link } from 'react-router-dom';
import { useMatchResumeToJob } from '../../hooks/useMatchResumeToJob';
import { useMatchSelectorOptions } from '../../hooks/useMatchSelectorOptions';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { Card } from '../../components/Card/Card';
import { Alert } from '../../components/Alert/Alert';
import { Button } from '../../components/Button/Button';
import { SelectField } from '../../components/TextField/SelectField';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { Spinner } from '../../components/Spinner/Spinner';
import { ScoreRing } from '../../components/ScoreRing/ScoreRing';
import { ScoreBar } from '../../components/ScoreBar/ScoreBar';
import { SkillTagList } from '../../components/SkillTagList/SkillTagList';
import { MatchIcon, UploadIcon, DocumentIcon } from '../../components/Icon/Icon';
import styles from './RunMatchPage.module.css';

export function RunMatchPage() {
  const {
    resumeId,
    jobDescriptionId,
    formError,
    status,
    result,
    isSubmitting,
    canSubmit,
    setResumeId,
    setJobDescriptionId,
    submit,
    reset,
  } = useMatchResumeToJob();

  const {
    status: selectorStatus,
    resumes,
    jobDescriptions,
    error: selectorError,
    reload: reloadSelectorOptions,
  } = useMatchSelectorOptions();

  async function handleSubmit(event) {
    event.preventDefault();
    await submit();
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
      title="Match resume to job description"
      description="See how well a resume aligns with a job description, scored by skills, experience, and overall semantic fit."
    >
      <Card className={styles.formCard}>
        {formError && <Alert tone="danger">{formError}</Alert>}

        {selectorStatus === 'loading' && <Spinner label="Loading your resumes and job descriptions\u2026" />}

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
                description="Upload a resume before you can run a match."
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
                description="Submit a job description before you can run a match."
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
          <form onSubmit={handleSubmit} noValidate>
            <div className={styles.idFields}>
              <SelectField
                label="Resume"
                name="resumeId"
                value={resumeId}
                onChange={(event) => setResumeId(event.target.value)}
                options={resumeOptions}
                disabled={isSubmitting}
                required
              />
              <SelectField
                label="Job description"
                name="jobDescriptionId"
                value={jobDescriptionId}
                onChange={(event) => setJobDescriptionId(event.target.value)}
                options={jobDescriptionOptions}
                disabled={isSubmitting}
                required
              />
            </div>

            <Button type="submit" isLoading={isSubmitting} disabled={!canSubmit} className={styles.submit}>
              Run match
            </Button>
          </form>
        )}
      </Card>

      {status === 'success' && result && (
        <Card className={styles.resultCard}>
          <div className={styles.scoreHeader}>
            <ScoreRing score={result.finalScore} label="Overall match" />
            <div className={styles.componentScores}>
              <ScoreBar label="Skills" score={result.skillScore} />
              <ScoreBar label="Experience" score={result.experienceScore} />
              <ScoreBar label="Semantic fit" score={result.semanticScore} />
            </div>
          </div>

          <div className={styles.skillsGrid}>
            <section aria-labelledby="matched-skills-heading" className={styles.skillsSection}>
              <h3 id="matched-skills-heading">Matched skills</h3>
              <SkillTagList
                skills={result.matchedSkills}
                tone="positive"
                emptyLabel="No matching skills were found."
              />
            </section>

            <section aria-labelledby="missing-skills-heading" className={styles.skillsSection}>
              <h3 id="missing-skills-heading">Missing skills</h3>
              <SkillTagList
                skills={result.missingSkills}
                tone="neutral"
                emptyLabel="No missing skills were identified."
              />
            </section>
          </div>

          <section aria-labelledby="explanation-heading" className={styles.explanationSection}>
            <h3 id="explanation-heading">Explanation</h3>
            <p className={styles.explanation}>{result.explanation}</p>
          </section>

          <Button onClick={reset} variant="secondary" className={styles.runAnother}>
            Run another match
          </Button>
        </Card>
      )}

      {selectorStatus === 'success' &&
        resumes.length > 0 &&
        jobDescriptions.length > 0 &&
        status === 'idle' &&
        !formError && (
          <EmptyState
            icon={<MatchIcon size={20} />}
            title="No match run yet"
            description="Select a resume and a job description above, then run the match to see the score breakdown."
          />
        )}
    </PageContainer>
  );
}
