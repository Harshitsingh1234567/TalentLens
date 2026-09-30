import { Link } from 'react-router-dom';
import { ArrowRightIcon } from '../Icon/Icon';
import styles from './QuickActionCard.module.css';

/**
 * One primary dashboard action: an icon, a title, a short description,
 * and a link to the page that does it. The whole card is the click
 * target (not just the trailing link text), with a single visible focus
 * ring so keyboard users get one clear outline rather than one on the
 * card and another on a nested link.
 */
export function QuickActionCard({ icon, title, description, to, cta }) {
  return (
    <Link to={to} className={styles.card}>
      <span className={styles.iconWrap}>{icon}</span>
      <span className={styles.body}>
        <span className={styles.title}>{title}</span>
        <span className={styles.description}>{description}</span>
      </span>
      <span className={styles.cta}>
        {cta}
        <ArrowRightIcon size={16} />
      </span>
    </Link>
  );
}
