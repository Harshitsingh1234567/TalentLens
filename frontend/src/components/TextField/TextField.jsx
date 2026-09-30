import { useId } from 'react';
import { classNames } from '../../utils/classNames';
import styles from './TextField.module.css';

/**
 * A labeled text input used for every form field in the app. The label
 * is always programmatically associated with the input via `htmlFor`/
 * `id` (using React's `useId` so every instance gets a unique id without
 * the caller having to invent one), and a validation error is linked via
 * `aria-describedby` + `aria-invalid` so assistive technology announces
 * it the same way a sighted user sees it, rather than only conveying
 * meaning through color.
 */
export function TextField({ label, error, hint, id, className, ...inputProps }) {
  const generatedId = useId();
  const inputId = id || generatedId;
  const errorId = error ? `${inputId}-error` : undefined;
  const hintId = hint ? `${inputId}-hint` : undefined;

  return (
    <div className={classNames(styles.field, className)}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
      </label>
      <input
        id={inputId}
        className={classNames(styles.input, error && styles.inputError)}
        aria-invalid={error ? true : undefined}
        aria-describedby={[hintId, errorId].filter(Boolean).join(' ') || undefined}
        {...inputProps}
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
