import { useEffect } from 'react';

const BASE_TITLE = 'TalentLens';

/**
 * Sets `document.title` for the current page and restores the previous
 * title on unmount, so navigating away (or an unmounted page left over
 * from a fast route change) never leaves a stale title behind.
 */
export function useDocumentTitle(pageTitle) {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = pageTitle ? `${pageTitle} - ${BASE_TITLE}` : BASE_TITLE;
    return () => {
      document.title = previousTitle;
    };
  }, [pageTitle]);
}
