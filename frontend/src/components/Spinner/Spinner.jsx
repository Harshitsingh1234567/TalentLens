import styles from './Spinner.module.css';

/**
 * A single loading indicator for the whole app. `label` is visually
 * hidden but always present for assistive technology - a spinning icon
 * alone conveys nothing to a screen reader.
 */
export function Spinner({ label = 'Loading…' }) {
  return (
    <div className={styles.wrapper} role="status">
      <span className={styles.spinner} aria-hidden="true" />
      <span className={styles.visuallyHidden}>{label}</span>
    </div>
  );
}
