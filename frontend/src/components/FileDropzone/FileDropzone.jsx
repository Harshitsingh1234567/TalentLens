import { useId, useState } from 'react';
import { classNames } from '../../utils/classNames';
import { Button } from '../Button/Button';
import { UploadIcon, FileTextIcon } from '../Icon/Icon';
import styles from './FileDropzone.module.css';

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
 * A single file-selection control used anywhere the app needs one file
 * from the user (currently: resume upload). Built on a native `<label>`
 * wrapping a `<input type="file">` - the standard accessible pattern for
 * a custom-styled file picker, since it gets full keyboard and screen
 * reader support for free, with drag-and-drop layered on top as a
 * progressive enhancement rather than a replacement for it.
 *
 * This component only handles *selecting* a file and describing it back
 * to the user (name, size) - it has no opinion on upload state,
 * validation rules, or what happens next; the caller decides all of
 * that, keeping this reusable across any future single-file upload need.
 */
export function FileDropzone({
  label,
  hint,
  error,
  accept,
  file,
  onFileSelected,
  onClear,
  disabled = false,
}) {
  const [isDraggingOver, setIsDraggingOver] = useState(false);
  const inputId = useId();
  const errorId = error ? `${inputId}-error` : undefined;
  const hintId = hint ? `${inputId}-hint` : undefined;

  function handleInputChange(event) {
    const selected = event.target.files && event.target.files[0];
    if (selected) {
      onFileSelected(selected);
    }
    // Reset so selecting the exact same file again (e.g. after clearing)
    // still fires a change event.
    event.target.value = '';
  }

  function handleDrop(event) {
    event.preventDefault();
    setIsDraggingOver(false);
    if (disabled) {
      return;
    }
    const dropped = event.dataTransfer.files && event.dataTransfer.files[0];
    if (dropped) {
      onFileSelected(dropped);
    }
  }

  return (
    <div className={styles.wrapper}>
      {label && <span className={styles.label}>{label}</span>}

      {file ? (
        <div className={styles.selectedFile}>
          <div className={styles.selectedFileLeading}>
            <span className={styles.selectedFileIcon}>
              <FileTextIcon size={18} />
            </span>
            <div className={styles.selectedFileInfo}>
              <span className={styles.fileName}>{file.name}</span>
              <span className={styles.fileMeta}>{formatFileSize(file.size)}</span>
            </div>
          </div>
          <Button type="button" variant="secondary" onClick={onClear} disabled={disabled}>
            Remove
          </Button>
        </div>
      ) : (
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
          <span className={styles.dropzoneIcon}>
            <UploadIcon size={22} />
          </span>
          <span className={styles.dropzonePrompt}>Drag and drop your file here, or click to browse</span>
          {accept && (
            <span className={styles.dropzoneFormats}>
              {accept
                .split(',')
                .map((extension) => extension.replace('.', '').toUpperCase())
                .join(' or ')}
            </span>
          )}
          <input
            id={inputId}
            type="file"
            accept={accept}
            onChange={handleInputChange}
            disabled={disabled}
            className={styles.input}
            aria-describedby={[hintId, errorId].filter(Boolean).join(' ') || undefined}
          />
        </label>
      )}

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
    </div>
  );
}
