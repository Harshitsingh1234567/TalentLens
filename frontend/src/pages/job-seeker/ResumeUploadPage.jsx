import { Link } from 'react-router-dom';
import { useResumeUpload } from '../../hooks/useResumeUpload';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { Card } from '../../components/Card/Card';
import { Alert } from '../../components/Alert/Alert';
import { Button } from '../../components/Button/Button';
import { TextField } from '../../components/TextField/TextField';
import { FileDropzone } from '../../components/FileDropzone/FileDropzone';
import styles from './ResumeUploadPage.module.css';

export function ResumeUploadPage() {
  const {
    file,
    fileError,
    candidateName,
    formError,
    status,
    result,
    isSubmitting,
    canSubmit,
    selectFile,
    clearFile,
    setCandidateName,
    submit,
    reset,
  } = useResumeUpload();

  async function handleSubmit(event) {
    event.preventDefault();
    await submit();
  }

  if (status === 'success' && result) {
    return (
      <PageContainer title="Upload resume">
        <Card>
          <Alert tone="success">Your resume was uploaded successfully.</Alert>
          <dl className={styles.summaryList}>
            <div>
              <dt>Resume ID</dt>
              <dd>{result.id}</dd>
            </div>
            <div>
              <dt>File</dt>
              <dd>{result.originalFilename}</dd>
            </div>
            {result.candidateName && (
              <div>
                <dt>Candidate name</dt>
                <dd>{result.candidateName}</dd>
              </div>
            )}
            <div>
              <dt>Uploaded</dt>
              <dd>{new Date(result.uploadedAt).toLocaleString()}</dd>
            </div>
          </dl>

          {result.extractedText && (
            <details className={styles.extractedTextDetails}>
              <summary>Preview extracted text</summary>
              <p className={styles.extractedTextHint}>
                This confirms what was read from your file - it isn't analyzed or scored yet.
              </p>
              <pre className={styles.extractedText}>{result.extractedText}</pre>
            </details>
          )}

          <div className={styles.actions}>
            <Button onClick={reset}>Upload another resume</Button>
            <Button as={Link} to="/job-seeker/dashboard" variant="secondary">
              Back to dashboard
            </Button>
          </div>
        </Card>
      </PageContainer>
    );
  }

  return (
    <PageContainer
      title="Upload resume"
      description="Upload a PDF or DOCX resume so it's on file for matching and analysis."
    >
      <Card>
        {formError && <Alert tone="danger">{formError}</Alert>}

        <form onSubmit={handleSubmit} noValidate>
          <FileDropzone
            label="Resume file"
            hint="PDF or DOCX, up to 5MB."
            error={fileError}
            accept=".pdf,.docx"
            file={file}
            onFileSelected={selectFile}
            onClear={clearFile}
            disabled={isSubmitting}
          />

          <TextField
            label="Candidate name (optional)"
            name="candidateName"
            value={candidateName}
            onChange={(event) => setCandidateName(event.target.value)}
            hint="Only needed if you're uploading on someone else's behalf."
            disabled={isSubmitting}
          />

          <Button type="submit" isLoading={isSubmitting} disabled={!canSubmit} className={styles.submit}>
            Upload resume
          </Button>
        </form>
      </Card>
    </PageContainer>
  );
}
