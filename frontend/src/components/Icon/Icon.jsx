/**
 * A single, consistent icon set for the whole product: one stroke style
 * (1.75px, round caps/joins, 24x24 viewBox), sized and colored entirely
 * via CSS (`currentColor` + font-size-relative `width`/`height`) rather
 * than baked-in fills. Kept as plain inline SVG - no icon library was
 * already part of this project, and this small, fixed set is lighter and
 * easier to keep visually consistent than adding one.
 *
 * Every icon is decorative by default (`aria-hidden`); anywhere an icon
 * stands in for a button/link with no visible text label, the caller is
 * responsible for an accessible name on that control, not on the icon.
 */
function Base({ children, size = 20, strokeWidth = 1.75, className }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      className={className}
    >
      {children}
    </svg>
  );
}

export function UploadIcon(props) {
  return (
    <Base {...props}>
      <path d="M12 15V4" />
      <path d="M7.5 8.5 12 4l4.5 4.5" />
      <path d="M4.5 15v3a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2v-3" />
    </Base>
  );
}

export function DocumentIcon(props) {
  return (
    <Base {...props}>
      <path d="M7 3.5h7l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 19V5A1.5 1.5 0 0 1 7 3.5Z" />
      <path d="M13.5 3.5V8H18" />
      <path d="M9 12.5h6" />
      <path d="M9 15.5h6" />
      <path d="M9 9.5h2" />
    </Base>
  );
}

export function MatchIcon(props) {
  return (
    <Base {...props}>
      <circle cx="9.5" cy="9.5" r="5.5" />
      <circle cx="14.5" cy="14.5" r="5.5" />
    </Base>
  );
}

export function AnalysisIcon(props) {
  return (
    <Base {...props}>
      <path d="M4 20V10.5" />
      <path d="M11 20V4" />
      <path d="M18 20v-6.5" />
    </Base>
  );
}

export function ImproveIcon(props) {
  return (
    <Base {...props}>
      <path d="M4 16.5 9.5 11l3.5 3.5L20 7" />
      <path d="M14.5 7h5.5v5.5" />
    </Base>
  );
}

export function CheckCircleIcon(props) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="8.25" />
      <path d="M8.75 12.25l2.25 2.25 4.25-4.75" />
    </Base>
  );
}

export function AlertCircleIcon(props) {
  return (
    <Base {...props}>
      <circle cx="12" cy="12" r="8.25" />
      <path d="M12 8v4.5" />
      <path d="M12 15.75h.01" />
    </Base>
  );
}

export function ChevronDownIcon(props) {
  return (
    <Base {...props}>
      <path d="M6 9.5 12 15l6-5.5" />
    </Base>
  );
}

export function ChevronRightIcon(props) {
  return (
    <Base {...props}>
      <path d="M9 6l6 6-6 6" />
    </Base>
  );
}

export function UserCircleIcon(props) {
  return (
    <Base {...props}>
      <circle cx="12" cy="9.75" r="3" />
      <path d="M5.5 19c1.3-2.9 3.8-4.5 6.5-4.5s5.2 1.6 6.5 4.5" />
      <circle cx="12" cy="12" r="8.25" />
    </Base>
  );
}

export function LogoutIcon(props) {
  return (
    <Base {...props}>
      <path d="M9.5 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h3.5" />
      <path d="M15.5 15.5 20 11l-4.5-4.5" />
      <path d="M20 11H9.5" />
    </Base>
  );
}

export function SparkleIcon(props) {
  return (
    <Base {...props}>
      <path d="M12 4.5c.5 2.6 1.4 3.9 4 4.5-2.6.6-3.5 1.9-4 4.5-.5-2.6-1.4-3.9-4-4.5 2.6-.6 3.5-1.9 4-4.5Z" />
      <path d="M18.5 15c.28 1.2.7 1.8 2 2.1-1.3.3-1.72.9-2 2.1-.28-1.2-.7-1.8-2-2.1 1.3-.3 1.72-.9 2-2.1Z" />
    </Base>
  );
}

export function CloseIcon(props) {
  return (
    <Base {...props}>
      <path d="M6 6l12 12" />
      <path d="M18 6 6 18" />
    </Base>
  );
}

export function MenuIcon(props) {
  return (
    <Base {...props}>
      <path d="M4 7h16" />
      <path d="M4 12h16" />
      <path d="M4 17h16" />
    </Base>
  );
}

export function FileTextIcon(props) {
  return (
    <Base {...props}>
      <path d="M7 3.5h7l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 19V5A1.5 1.5 0 0 1 7 3.5Z" />
      <path d="M13.5 3.5V8H18" />
      <path d="M9 13h6" />
      <path d="M9 16h4" />
    </Base>
  );
}

export function SearchIcon(props) {
  return (
    <Base {...props}>
      <circle cx="10.5" cy="10.5" r="6" />
      <path d="M19 19l-4.3-4.3" />
    </Base>
  );
}

export function ArrowRightIcon(props) {
  return (
    <Base {...props}>
      <path d="M4 12h15.5" />
      <path d="M14 6.5 19.5 12 14 17.5" />
    </Base>
  );
}
