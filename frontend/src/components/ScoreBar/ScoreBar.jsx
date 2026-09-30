import { useId } from 'react';
import { classNames } from '../../utils/classNames';
import styles from './ScoreBar.module.css';

/**
 * Displays one 0-1 score (skillScore, experienceScore, semanticScore, or
 * finalScore from MatchResponse) as a labeled percentage with a visual
 * bar. Built on the native `<progress>` element rather than a hand-rolled
 * div-based bar with manual ARIA - `<progress>` already exposes the
 * correct role and value to assistive technology for free.
 */
export function ScoreBar({ label, score, size = 'md' }) {
  const labelId = useId();
  const percentage = Math.round(score * 100);

  return (
    <div className={classNames(styles.wrapper, size === 'lg' && styles.large)}>
      <div className={styles.labelRow}>
        <span id={labelId} className={styles.label}>
          {label}
        </span>
        <span className={styles.percentage}>{percentage}%</span>
      </div>
      <progress className={styles.bar} value={score} max={1} aria-labelledby={labelId} />
    </div>
  );
}
