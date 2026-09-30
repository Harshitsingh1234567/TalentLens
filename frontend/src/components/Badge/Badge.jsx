import { classNames } from '../../utils/classNames';
import styles from './Badge.module.css';

/**
 * A small pill label - used for the "AI-enhanced" indicator on analysis
 * content and any other short status tag. `tone="insight"` is reserved
 * specifically for AI-derived content (see tokens.css), so that accent
 * color always means the same thing wherever a Badge appears.
 */
export function Badge({ tone = 'neutral', icon, children, className }) {
  return (
    <span className={classNames(styles.badge, styles[tone], className)}>
      {icon}
      {children}
    </span>
  );
}
