import { useId } from 'react';
import { classNames } from '../../utils/classNames';
import styles from './ScoreRing.module.css';

const RADIUS = 54;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

function toneForScore(score) {
  if (score >= 0.7) return 'positive';
  if (score >= 0.4) return 'neutral';
  return 'attention';
}

/**
 * The single dominant score visualization for the Match and Analysis
 * pages (MatchResponse.finalScore / AnalysisResponse.finalScore) - a
 * radial progress ring built on plain SVG rather than a charting
 * library, since it only ever displays one value. `ScoreBar` remains the
 * component for the three secondary component scores shown alongside
 * it, so the one ring stays visually dominant rather than one of four
 * identical shapes.
 */
export function ScoreRing({ score, label = 'Overall match' }) {
  const labelId = useId();
  const clamped = Math.max(0, Math.min(1, score));
  const percentage = Math.round(clamped * 100);
  const offset = CIRCUMFERENCE * (1 - clamped);
  const tone = toneForScore(clamped);

  return (
    <div className={styles.wrapper} role="img" aria-labelledby={labelId}>
      <svg viewBox="0 0 120 120" className={styles.svg}>
        <circle cx="60" cy="60" r={RADIUS} className={styles.track} />
        <circle
          cx="60"
          cy="60"
          r={RADIUS}
          className={classNames(styles.value, styles[tone])}
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={offset}
        />
      </svg>
      <div className={styles.center}>
        <span className={classNames(styles.percentage, 'tabular-nums')}>{percentage}%</span>
        <span id={labelId} className={styles.label}>
          {label}
        </span>
      </div>
    </div>
  );
}
