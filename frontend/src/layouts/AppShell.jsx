import { useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { ROLES } from '../auth/roles';
import { LogoutIcon, MenuIcon, CloseIcon } from '../components/Icon/Icon';
import { BrandMark } from '../components/BrandMark/BrandMark';
import { classNames } from '../utils/classNames';
import styles from './AppShell.module.css';

const NAV_LINKS_BY_ROLE = {
  [ROLES.JOB_SEEKER]: [
    { to: '/job-seeker/dashboard', label: 'Dashboard' },
    { to: '/job-seeker/resumes/upload', label: 'Resume' },
    { to: '/job-seeker/job-descriptions/new', label: 'Job Description' },
    { to: '/job-seeker/matches/new', label: 'Match' },
    { to: '/job-seeker/analysis', label: 'Analysis' },
  ],
  [ROLES.RECRUITER]: [
    { to: '/recruiter/dashboard', label: 'Dashboard' },
    { to: '/recruiter/screening-batches', label: 'Screening Batches' },
    { to: '/recruiter/candidate-ranking', label: 'Candidate Ranking' },
  ],
};

function initialsFor(email) {
  if (!email) return '';
  return email.charAt(0).toUpperCase();
}

/**
 * The persistent frame every authenticated page renders inside: a header
 * with the product mark, role-aware navigation links, the signed-in
 * user's identity, and a logout action, followed by the routed page
 * content via `<Outlet />`. Navigation collapses into a disclosure menu
 * below the `md` breakpoint rather than wrapping the same layout, and
 * closes automatically on route change so it never persists open across
 * a navigation.
 */
export function AppShell() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
  const [lastPathname, setLastPathname] = useState(location.pathname);
  const navLinks = (user && NAV_LINKS_BY_ROLE[user.role]) || [];

  if (location.pathname !== lastPathname) {
    setLastPathname(location.pathname);
    setIsMobileNavOpen(false);
  }

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.brand}>
            <span className={styles.brandMark} aria-hidden="true">
              <BrandMark size={23} />
            </span>
            <span className={styles.brandName}>TalentLens</span>
          </div>

          {navLinks.length > 0 && (
            <nav className={classNames(styles.nav, isMobileNavOpen && styles.navOpen)} aria-label="Primary">
              {navLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) => classNames(styles.navLink, isActive && styles.navLinkActive)}
                >
                  {link.label}
                </NavLink>
              ))}
            </nav>
          )}

          <div className={styles.userArea}>
            {user && (
              <span className={styles.userChip}>
                <span className={styles.userAvatar} aria-hidden="true">
                  {initialsFor(user.email)}
                </span>
                <span className={styles.userEmail}>{user.email}</span>
              </span>
            )}
            <button type="button" className={styles.logoutButton} onClick={logout}>
              <LogoutIcon size={17} />
              <span>Log out</span>
            </button>

            {navLinks.length > 0 && (
              <button
                type="button"
                className={styles.navToggle}
                aria-expanded={isMobileNavOpen}
                aria-label={isMobileNavOpen ? 'Close navigation menu' : 'Open navigation menu'}
                onClick={() => setIsMobileNavOpen((open) => !open)}
              >
                {isMobileNavOpen ? <CloseIcon size={20} /> : <MenuIcon size={20} />}
              </button>
            )}
          </div>
        </div>
      </header>

      <Outlet />
    </div>
  );
}
