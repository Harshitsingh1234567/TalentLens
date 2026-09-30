import { useId, useState } from 'react';
import { classNames } from '../../utils/classNames';
import { Button } from '../Button/Button';
import styles from './MultiFileDropzone.module.css';

function formatFileSize(bytes) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * The multi-file counterpart to `FileDropzone` (which stays single-file,
 * for the one-resume Job Seeker upload it already serves) - built for
 * assembling a batch of files across one or more picks/drops, each
 * individually removable before submitting. Shares the same accessible
 * foundation: a real, full-size, focusable `<input type="file" multiple>`
 * rather than a purely decorative one, with drag-and-drop layered on top
 * as a progressive enhancement.
 *
 * This intentionally does no per-file validation (extension/size) - the
 * backend already reports per-file failures individually without
 * rejecting the whole batch, which is the behavior this feature is
 * built to surface, not pre-empt.
 */
export function MultiFileDropzone({
  label,
  hint,
  error,
  accept,
  files,
  onFilesSelected,
  onRemoveFile,
  disabled = false,
}) {
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const inputId = useId();
  const errorId = error ? `${inputId}-error` : undefined;
  const hintId = hint ? `${inputId}-hint` : undefined;

  function handleInputChange(event) {
    const selected = event.target.files;
    if (selected && selected.length > 0) {
      onFilesSelected(Array.from(selected));
    }
    // Reset so picking the exact same file(s) again still fires a change event.
    event.target.value = '';
  }

  function handleDrop(event) {
    event.preventDefault();
    setIsDraggingOver(false);
    if (disabled) {
      return;
    }
    const dropped = event.dataTransfer.files;
    if (dropped && dropped.length > 0) {
      onFilesSelected(Array.from(dropped));
    }
  }

  return (
    <div className={styles.wrapper}>
      {label && <span className={styles.label}>{label}</span>}

      <label
        htmlFor={inputId}
        className={classNames(
          styles.dropzone,
          isDraggingOver && styles.dropzoneActive,
          error && styles.dropzoneError,
          disabled && styles.dropzoneDisabled,
        )}
        onDragOver={(event) => {
          event.preventDefault();
          if (!disabled) {
            setIsDraggingOver(true);
          }
        }}
        onDragLeave={() => setIsDraggingOver(false)}
        onDrop={handleDrop}
      >
        <span className={styles.dropzonePrompt}>
          Drag and drop resume files here, or click to browse. You can add more than one at a time.
        </span>
        <input
          id={inputId}
          type="file"
          accept={accept}
          multiple
          onChange={handleInputChange}
          disabled={disabled}
          className={styles.input}
          aria-describedby={[hintId, errorId].filter(Boolean).join(' ') || undefined}
        />
      </label>

      {hint && !error && (
        <p id={hintId} className={styles.hint}>
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className={styles.error} role="alert">
          {error}
        </p>
      )}

      {files.length > 0 && (
        <ul className={styles.fileList}>
          {files.map((file, index) => (
            <li key={`${file.name}-${file.size}-${index}`} className={styles.fileItem}>
              <div className={styles.fileInfo}>
                <span className={styles.fileName}>{file.name}</span>
                <span className={styles.fileMeta}>{formatFileSize(file.size)}</span>
              </div>
              <Button
                type="button"
                variant="secondary"
                onClick={() => onRemoveFile(index)}
                disabled={disabled}
              >
                Remove
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
