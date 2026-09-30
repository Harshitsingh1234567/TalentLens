import { classNames } from '../../utils/classNames';
import { CheckCircleIcon, AlertCircleIcon } from '../Icon/Icon';
import styles from './Alert.module.css';

const ROLE_BY_TONE = {
  danger: 'alert',
  warning: 'alert',
  success: 'status',
  info: 'status',
};

const ICON_BY_TONE = {
  danger: AlertCircleIcon,
  warning: AlertCircleIcon,
  success: CheckCircleIcon,
  info: AlertCircleIcon,
};

/**
 * A single alert component for every inline status message in the app
 * (form submission errors, empty-state notices, etc.), so error styling
 * is never re-invented per page. `tone` maps to the visual style, the
 * correct ARIA role, and a matching icon: `danger`/`warning` use
 * `role="alert"` (an assertive live region, appropriate for something
 * that needs immediate attention), `success`/`info` use the calmer
 * `role="status"`. The icon is decorative - the tone and message text
 * are never conveyed by color/icon alone.
 */
export function Alert({ tone = 'info', children, className }) {
  const ToneIcon = ICON_BY_TONE[tone];

  return (
    <div className={classNames(styles.alert, styles[tone], className)} role={ROLE_BY_TONE[tone]}>
      <ToneIcon size={18} className={styles.icon} />
      <span>{children}</span>
    </div>
  );
}
