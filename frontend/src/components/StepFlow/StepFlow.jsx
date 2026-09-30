import { Link } from 'react-router-dom';
import { classNames } from '../../utils/classNames';
import styles from './StepFlow.module.css';

/**
 * The dashboard's visual backbone: the real, ordered steps the product
 * requires (upload a resume, add a job description, run a match, review
 * analysis, act on the suggestions). Rendered as an `<ol>` because this
 * is a genuine sequence, not decorative numbering - each step really
 * does depend on the one before it. Steps with a `to` are real
 * navigable actions; a step without one (the final "Improve" step) is
 * the outcome of following the flow rather than a separate page, so it
 * renders as a plain, non-interactive terminus instead of a route that
 * doesn't exist.
 */
export function StepFlow({ steps }) {
  return (
    <ol className={styles.flow}>
      {steps.map((step, index) => {
        const content = (
          <>
            <span className={styles.iconWrap}>
              <span className={styles.icon}>{step.icon}</span>
              <span className={styles.number}>{index + 1}</span>
            </span>
            <span className={styles.text}>
              <span className={styles.title}>{step.title}</span>
              <span className={styles.description}>{step.description}</span>
            </span>
          </>
        );

        return (
          <li key={step.title} className={styles.item}>
            {step.to ? (
              <Link to={step.to} className={styles.stepLink}>
                {content}
              </Link>
            ) : (
              <div className={classNames(styles.stepLink, styles.stepTerminal)}>{content}</div>
            )}
          </li>
        );
      })}
    </ol>
  );
}
