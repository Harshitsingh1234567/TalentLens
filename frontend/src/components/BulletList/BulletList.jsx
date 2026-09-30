import styles from './BulletList.module.css';

const COLLAPSE_THRESHOLD = 6;

/**
 * Renders a labeled list of plain-string items (ATS issues, formatting
 * issues, content suggestions, and their AI-enhanced counterparts all
 * share this exact shape in AnalysisResponse) with one consistent empty
 * state, instead of repeating heading+ul+li+empty-check per section.
 *
 * Long lists (more than a handful of items) render inside a native
 * `<details>` so the section stays scannable without losing anything -
 * expanded by default (nothing is hidden unless the reader chooses to
 * collapse it), matching the same accessible disclosure pattern already
 * used for the resume text preview on the upload page. Short lists stay
 * a plain list; a two-item section doesn't need a disclosure control.
 */
export function BulletList({ title, items, emptyLabel }) {
  const hasItems = items && items.length > 0;
  const list = hasItems ? (
    <ul className={styles.list}>
      {items.map((item, index) => (
        <li key={index}>{item}</li>
      ))}
    </ul>
  ) : (
    <p className={styles.empty}>{emptyLabel}</p>
  );

  if (hasItems && items.length > COLLAPSE_THRESHOLD) {
    return (
      <details className={styles.section} open>
        <summary className={styles.heading}>
          {title} <span className={styles.count}>({items.length})</span>
        </summary>
        {list}
      </details>
    );
  }

  return (
    <div className={styles.section}>
      <h4 className={styles.heading}>{title}</h4>
      {list}
    </div>
  );
}
