import styles from './EmptyState.module.css';

/**
 * A single empty-state component for any list/page with nothing in it
 * yet. Always pairs a plain description of the current state with an
 * optional concrete next action, rather than leaving a bare "No data".
 * `icon` is optional and purely decorative.
 */
export function EmptyState({ icon, title, description, action }) {
  return (
    <div className={styles.wrapper}>
      {icon && <div className={styles.icon}>{icon}</div>}
      <h3 className={styles.title}>{title}</h3>
      {description && <p className={styles.description}>{description}</p>}
      {action && <div className={styles.action}>{action}</div>}
    </div>
  );
}
