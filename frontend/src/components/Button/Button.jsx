import { classNames } from '../../utils/classNames';
import styles from './Button.module.css';

/**
 * A single button component covering every button-styled control in the
 * app, rather than one-off styled buttons per page. `variant` covers the
 * visual hierarchy (primary action vs a lower-emphasis secondary action
 * vs a destructive one); everything else is standard `<button>` behavior.
 *
 * `as` lets this render as a different component (typically React
 * Router's `Link`) while keeping identical visual styling - e.g.
 * `<Button as={Link} to="/login">Log in</Button>`. This exists
 * specifically so a link that is visually a button never has to be
 * built by nesting an actual `<button>` inside an `<a>`, which creates
 * two overlapping interactive/focusable elements for what is meant to be
 * one control - a real accessibility and event-handling defect, not just
 * a style concern. When `as` is omitted (the default, and every existing
 * usage), behavior is unchanged: a plain `<button>`.
 */
export function Button({
  as: Component = 'button',
  variant = 'primary',
  type = 'button',
  isLoading = false,
  disabled = false,
  children,
  className,
  ...rest
}) {
  const isNativeButton = Component === 'button';

  return (
    <Component
      // A non-button "as" target (e.g. Link) has no `type` attribute and
      // must not be disabled the same way a native button is - it stays
      // navigable, but visually/ARIA-communicates its disabled-like state
      // via aria-disabled instead, since anchors have no `disabled` DOM
      // property.
      type={isNativeButton ? type : undefined}
      disabled={isNativeButton ? disabled || isLoading : undefined}
      aria-disabled={!isNativeButton && (disabled || isLoading) ? true : undefined}
      className={classNames(styles.button, styles[variant], className)}
      aria-busy={isLoading || undefined}
      {...rest}
    >
      {isLoading ? 'Please wait…' : children}
    </Component>
  );
}
