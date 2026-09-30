import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import styles from './PageContainer.module.css';

/**
 * Every page renders its content inside this, rather than each page
 * re-implementing its own width/padding/heading layout. Also sets the
 * document title, so pages don't have to remember to do that themselves.
 */
export function PageContainer({ title, description, children }) {
  useDocumentTitle(title);

  return (
    <main className={styles.container}>
      {(title || description) && (
        <div className={styles.header}>
          {title && <h1>{title}</h1>}
          {description && <p>{description}</p>}
        </div>
      )}
      {children}
    </main>
  );
}
