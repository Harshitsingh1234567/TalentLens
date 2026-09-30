import { useId } from 'react';
import { classNames } from '../../utils/classNames';
import styles from './TextField.module.css';

/**
 * A labeled multi-line text control (used for job description content),
 * sharing TextField's visual language and label/error/hint association
 * pattern rather than being a one-off. The caller is responsible for any
 * character-count display (passed via `hint`) - this component only
 * renders the field, it has no opinion on what a "long" value is for any
 * particular use case.
 */
export function TextAreaField({ label, error, hint, id, className, ...textareaProps }) {
  const generatedId = useId();
  const textareaId = id || generatedId;
  const errorId = error ? `${textareaId}-error` : undefined;
  const hintId = hint ? `${textareaId}-hint` : undefined;

  return (
    <div className={classNames(styles.field, className)}>
      <label className={styles.label} htmlFor={textareaId}>
        {label}
      </label>
      <textarea
        id={textareaId}
        className={classNames(styles.input, styles.textarea, error && styles.inputError)}
        aria-invalid={error ? true : undefined}
        aria-describedby={[hintId, errorId].filter(Boolean).join(' ') || undefined}
        {...textareaProps}
      />
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
