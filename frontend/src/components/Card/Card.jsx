import { classNames } from '../../utils/classNames';
import styles from './Card.module.css';

/**
 * A bordered content surface used for forms, dashboard panels, and
 * anything else that needs to visually group content. Flat with a
 * precise border rather than a heavy drop shadow, in keeping with the
 * app's overall "precision tool" visual language.
 */
export function Card({ children, className, ...rest }) {
  return (
    <div className={classNames(styles.card, className)} {...rest}>
      {children}
    </div>
  );
}
