/**
 * Joins truthy class name fragments with a space, skipping falsy ones.
 * Used throughout the component library to combine a CSS Module's base
 * class with conditional variant classes, e.g.:
 *   classNames(styles.button, variant === 'danger' && styles.danger)
 */
export function classNames(...values) {
  return values.filter(Boolean).join(' ');
}
