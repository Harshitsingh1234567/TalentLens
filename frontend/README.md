# Resume Analyzer - Frontend

React + Vite (JavaScript, no TypeScript) frontend for the Resume Analyzer backend under `../backend`.

This is the Phase 8 **foundation**: authentication, role-based routing, the API layer, and a small
reusable component/design-system library. It intentionally does not yet include the resume upload,
job description, matching, analysis, screening-batch, or candidate-ranking screens - those consume
this foundation in later phases.

## Requirements

- Node.js 20+ and npm
- The backend running locally (see `../backend/README.md`), by default on `http://localhost:8080`

## Getting started

```bash
cp .env.example .env.local   # optional - defaults already work for local dev
npm install
npm run dev
```

Open the URL Vite prints (typically `http://localhost:5173`).

### Why API calls work without CORS changes to the backend

The backend (frozen as of Phase 7) does not send CORS headers. Rather than requiring a backend
change, the Vite dev server proxies any request starting with `/api` straight through to the backend
(see `vite.config.js`), so the browser only ever talks to the dev server's own origin. As long as
`VITE_API_BASE_URL` in your `.env.local` is left empty (the default), this "just works" for local
development. See `.env.example` for the full explanation and the production-deployment note.

## Available scripts

- `npm run dev` - start the Vite dev server
- `npm run build` - production build to `dist/`
- `npm run preview` - locally preview the production build
- `npm run lint` - run `oxlint` (zero-config, fast; no ESLint setup needed)

## Project structure

```
src/
  api/         Centralized fetch client, ApiError, and the two auth endpoint calls
  auth/        Auth context/provider, session persistence, role constants, JWT decode helper
  components/  Small reusable UI components (Button, TextField, Card, Alert, Spinner, ...)
  hooks/       Small reusable hooks (useDocumentTitle)
  layouts/     AppShell (header, nav, logout) wrapping authenticated pages
  pages/       Route-level pages (Login, Register, dashboards, error pages)
  routes/      Route tree and the ProtectedRoute / RoleRoute guards
  styles/      Design tokens (CSS custom properties) and global base styles
  utils/       Small framework-agnostic helpers (e.g. classNames)
```

## Roles

The backend defines exactly two roles, `JOB_SEEKER` and `RECRUITER` (see `src/auth/roles.js`, which
mirrors `com.resumeanalyzer.entity.Role` exactly). There is no admin role.
