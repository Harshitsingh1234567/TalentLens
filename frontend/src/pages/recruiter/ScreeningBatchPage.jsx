import { useScreeningBatchWorkflow } from '../../hooks/useScreeningBatchWorkflow';
import { PageContainer } from '../../components/PageContainer/PageContainer';
import { Card } from '../../components/Card/Card';
import { Alert } from '../../components/Alert/Alert';
import { Button } from '../../components/Button/Button';
import { TextField } from '../../components/TextField/TextField';
import { EmptyState } from '../../components/EmptyState/EmptyState';
import { MultiFileDropzone } from '../../components/MultiFileDropzone/MultiFileDropzone';
import { BulletList } from '../../components/BulletList/BulletList';
import styles from './ScreeningBatchPage.module.css';

function BatchResult({ result }) {
  const successCount = result.resumes.length;
  const failureCount = result.failedFiles.length;
  const totalCount = successCount + failureCount;

  return (
    <Card className={styles.resultCard}>
      <div className={styles.resultMeta}>
        <span>
          Batch ID: <strong>{result.id}</strong>
        </span>
        <span>Name: {result.name}</span>
        <span>{new Date(result.createdAt).toLocaleString()}</span>
      </div>

      {totalCount > 0 && (
        <Alert tone={failureCount > 0 ? 'warning' : 'success'}>
          {failureCount > 0
            ? `${successCount} of ${totalCount} file${totalCount === 1 ? '' : 's'} were added successfully.`
            : `All ${successCount} file${successCount === 1 ? '' : 's'} were added successfully.`}
        </Alert>
      )}

      <section aria-labelledby="batch-resumes-heading" className={styles.section}>
        <h3 id="batch-resumes-heading">Resumes in this batch</h3>
        {result.resumes.length > 0 ? (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th scope="col">Resume ID</th>
                  <th scope="col">Candidate</th>
                  <th scope="col">File</th>
                  <th scope="col">Type</th>
                  <th scope="col">Uploaded</th>
                </tr>
              </thead>
              <tbody>
                {result.resumes.map((resume) => (
                  <tr key={resume.resumeId}>
                    <td>{resume.resumeId}</td>
                    <td>{resume.candidateName || '\u2014'}</td>
                    <td className={styles.filenameCell}>{resume.originalFilename}</td>
                    <td>{resume.fileType}</td>
                    <td>{new Date(resume.uploadedAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className={styles.empty}>No resumes were added to this batch.</p>
        )}
      </section>

      {result.failedFiles.length > 0 && (
        <BulletList
          title="Files that couldn't be processed"
          items={result.failedFiles.map((failure) => `${failure.originalFilename}: ${failure.reason}`)}
        />
      )}
    </Card>
  );
}

export function ScreeningBatchPage() {
  const {
    name,
    files,
    lookupBatchId,
    nameError,
    formError,
    status,
    result,
    isSubmitting,
    maxFilesPerBatch,
    canCreateBatch,
    canLoadBatch,
    setName,
    addFiles,
    removeFile,
    setLookupBatchId,
    createBatch,
    loadBatch,
    reset,
  } = useScreeningBatchWorkflow();

  async function handleCreateBatch(event) {
    event.preventDefault();
    await createBatch();
  }

  async function handleLoadBatch(event) {
    event.preventDefault();
    await loadBatch();
  }

  return (
    <PageContainer
      title="Screening batches"
      description="Upload multiple resumes at once as a named batch for later candidate screening."
    >
      <div className={styles.formsGrid}>
        <Card>
          <h2>Create a screening batch</h2>
          <form onSubmit={handleCreateBatch} noValidate>
            <TextField
              label="Batch name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              error={nameError}
              disabled={isSubmitting}
              required
            />
            <MultiFileDropzone
              label="Resume files"
              hint={`PDF or DOCX, up to 5MB each, up to ${maxFilesPerBatch} files per batch.`}
              accept=".pdf,.docx"
              files={files}
              onFilesSelected={addFiles}
              onRemoveFile={removeFile}
              disabled={isSubmitting}
            />
            <Button type="submit" isLoading={isSubmitting} disabled={!canCreateBatch}>
              Create batch
            </Button>
          </form>
        </Card>

        <Card>
          <h2>View an existing batch</h2>
          <form onSubmit={handleLoadBatch} noValidate>
            <TextField
              label="Batch ID"
              type="number"
              inputMode="numeric"
              min="1"
              step="1"
              value={lookupBatchId}
              onChange={(event) => setLookupBatchId(event.target.value)}
              hint="Shown after you create a batch."
              disabled={isSubmitting}
              required
            />
            <Button type="submit" isLoading={isSubmitting} disabled={!canLoadBatch} variant="secondary">
              Load batch
            </Button>
          </form>
        </Card>
      </div>

      {formError && <Alert tone="danger">{formError}</Alert>}

      {status === 'success' && result && (
        <>
          <BatchResult result={result} />
          <Button onClick={reset} variant="secondary" className={styles.startOver}>
            Start over
          </Button>
        </>
      )}

      {status === 'idle' && !formError && (
        <EmptyState
          title="No batch loaded yet"
          description="Create a new screening batch or load an existing one by its ID to see its details."
        />
      )}
    </PageContainer>
  );
}
