/**
 * The TalentLens brand mark: a rounded badge in the primary brand color
 * containing a simple lens glyph (ring + focal point + short handle),
 * rendered in the surface color. Used wherever the wordmark appears -
 * the navbar and both auth pages - so the icon is defined once. Sized
 * via the `size` prop; color comes from `currentColor` on the badge, so
 * it inherits whatever text color is set on an ancestor (matching the
 * rest of the icon set's convention).
 */
export function BrandMark({ size = 22 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false">
      <rect x="1" y="1" width="22" height="22" rx="7" fill="currentColor" />
      <circle cx="10.25" cy="10.25" r="4.25" stroke="var(--color-surface)" strokeWidth="1.7" />
      <circle cx="10.25" cy="10.25" r="1" fill="var(--color-surface)" />
      <path d="M13.5 13.5 17 17" stroke="var(--color-surface)" strokeWidth="1.7" strokeLinecap="round" />
    </svg>
  );
}
