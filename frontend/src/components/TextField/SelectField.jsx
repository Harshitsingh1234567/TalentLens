import { useId } from 'react';
import { classNames } from '../../utils/classNames';
import styles from './TextField.module.css';

/**
 * A labeled select control sharing TextField's visual language and
 * label/error association pattern, so the two form controls in the app
 * (text input, role picker) feel like one consistent system rather than
 * two different ad hoc implementations.
 */
export function SelectField({ label, error, options, id, className, ...selectProps }) {
  const generatedId = useId();
  const selectId = id || generatedId;
  const errorId = error ? `${selectId}-error` : undefined;

  return (
    <div className={classNames(styles.field, className)}>
      <label className={styles.label} htmlFor={selectId}>
        {label}
      </label>
      <select
        id={selectId}
        className={classNames(styles.input, error && styles.inputError)}
        aria-invalid={error ? true : undefined}
        aria-describedby={errorId}
        {...selectProps}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && (
        <p id={errorId} className={styles.error} role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
