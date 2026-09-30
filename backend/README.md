# AI-Powered Resume & Job Match Analyzer

## Purpose

A role-based web application that helps two users of the hiring process:

- **Job Seeker** — upload a single resume and a target Job Description to
  receive a transparent match score, ATS-friendliness report, formatting
  feedback, AI-assisted content suggestions, and interview-question
  preparation.
- **HR/Recruiter** — provide a Job Description and bulk-upload multiple
  candidate resumes to receive a ranked, comparable screening view. The
  system provides screening assistance only; it does not make the final
  hiring decision.

The system combines deterministic, rule-based NLP (resume/JD parsing,
keyword extraction, ATS/formatting checks) with a narrowly-scoped
generative-AI layer (Google Gemini API) used only for embeddings-based
semantic similarity, content-rewrite suggestions, and interview-question
generation.

## Technology Stack

- **Backend:** Java 21, Spring Boot 3.x, Maven
- **Frontend:** React.js (not yet started)
- **Database:** MySQL
- **Security:** Spring Security + JWT
- **Resume parsing:** Apache PDFBox (PDF), Apache POI (DOCX)
- **NLP:** Apache OpenNLP
- **Generative AI:** Google Gemini API (official `google-genai` Java SDK) - Phase 5
- **Testing:** JUnit 5, Mockito

## Current Development Phase: Phase 0 — Project Foundation

Phase 0 establishes a clean, buildable backend skeleton only. It contains
**no authentication, no business logic, and no REST endpoints** beyond
what Spring Boot itself requires to start.

### What Phase 0 contains

- Spring Boot 3.x application bootstrap (`ResumeAnalyzerApplication`)
- MySQL datasource + JPA/Hibernate configuration (`application.yml`)
- `User` entity (id, email, password hash field, role) and `Role` enum
  (`JOB_SEEKER`, `RECRUITER` — no `ADMIN`)
- `UserRepository` (Spring Data JPA) with only the lookup methods needed
  for the planned Phase 1 authentication workflow
- Minimal Spring Security scaffolding (`SecurityConfig`) so the app starts
  cleanly with Spring Security on the classpath, with all requests
  currently permitted (no protected endpoints exist yet)
- Externalized match-score weighting configuration
  (`MatchingWeightsProperties`), bound to `matching.weights.*` in
  `application.yml`, defaulting to 0.34 / 0.33 / 0.33
- A minimal global exception-handling foundation (`GlobalExceptionHandler`)
- A single test verifying the Spring application context loads
  successfully (run against an in-memory H2 database so it does not
  require a live MySQL server)

### What is intentionally NOT implemented yet

- No JWT generation/validation, no login/register logic, no password
  hashing logic
- No REST endpoints of any kind (including no health-check endpoint)
- No `Resume`, `JobDescription`, `Analysis`, `ScreeningBatch`,
  `ContentSuggestion`, or `InterviewQuestion` entities
- No resume parsing (PDFBox/POI), no NLP (OpenNLP/Lucene)
- No matching/scoring logic, no ATS analysis, no formatting/readability
  analysis
- No Gemini API integration
- No React frontend
- No recruiter or Job Seeker workflow logic

These arrive incrementally in later phases, per the agreed phase plan.

## Building and Running

### Prerequisites

- JDK 21
- Maven 3.9+ (or use the included wrapper, if added later)
- A running MySQL 8.x server (only needed to actually *run* the app; not
  needed to compile or run the test suite)

### Build

```bash
mvn clean compile
```

### Run tests

```bash
mvn test
```

### Run the application

```bash
mvn spring-boot:run
```

## MySQL Configuration

The application reads its datasource configuration from
`src/main/resources/application.yml`, using environment variables with
sensible local-development defaults:

| Variable      | Default             | Description                          |
|---------------|----------------------|---------------------------------------|
| `DB_HOST`     | `localhost`          | MySQL host                            |
| `DB_PORT`     | `3306`               | MySQL port                            |
| `DB_NAME`     | `resume_analyzer`    | Database name (auto-created if absent)|
| `DB_USERNAME` | `root`               | MySQL username                        |
| `DB_PASSWORD` | *(empty)*            | MySQL password                        |

No real credentials are hard-coded in the repository. Set these
environment variables (or override them in a local, untracked
configuration) before running the application against your own MySQL
instance.

`spring.jpa.hibernate.ddl-auto` is currently set to `update`, which is
appropriate for local development while the schema is still small
(Phase 0 only defines the `users` table). This should be revisited later
if/when the project introduces a schema migration tool.

## Phase 5 — AI/Gemini Enhancement

Phase 5 adds an optional, official Google Gemini integration on top of
the frozen Phase 0–4 deterministic pipeline. It never replaces or
modifies any Phase 3 (matching/scoring) or Phase 4 (basic ATS/content-
suggestion/interview-question) logic - it only adds a second, clearly
separated layer of AI-generated insights alongside it.

### What Phase 5 adds

- Official Google Gen AI Java SDK integration (`com.google.genai:google-genai`),
  isolated entirely behind `com.resumeanalyzer.ai.client.GeminiTextGenerationClient`
  - the only class in the project that imports the SDK.
- A provider-agnostic seam (`AiTextGenerationClient`, `AiAnalysisEnhancer`)
  that the rest of the application depends on, so the concrete provider
  is swappable without touching `AnalysisService` or any controller.
- Centralized, versioned prompt template (`AnalysisEnhancementPrompt`),
  producing one Gemini call per `POST /api/analysis` covering:
  - advanced ATS/keyword-optimization insights
  - formatting/readability insights
  - context-aware content suggestions
  - context-aware interview questions
- Strict validation/sanitization (`AiResponseSanitizer`) of the returned
  JSON before anything is persisted (size caps, dedup, blank/invalid
  entries dropped).
- A safe, deterministic fallback: if Gemini is disabled, unconfigured,
  times out, errors, or returns an invalid/empty response, the analysis
  is still created successfully from the Phase 3/4 deterministic data
  alone - the AI sections are simply empty and `aiEnhancementStatus`
  reflects why (`DISABLED` / `FALLBACK` / `SUCCESS`).
- A new read-only endpoint, `GET /api/analysis/{id}/interview-questions`,
  returning both the deterministic and (when available) AI-generated
  interview questions already stored on the analysis.

### Configuration

Bound to `ai.gemini.*` (see `GeminiProperties`), all overridable via
environment variables - no key is ever hard-coded:

| Variable                  | Default              | Description                                   |
|----------------------------|----------------------|------------------------------------------------|
| `GEMINI_ENABLED`           | `true`                | Feature switch                                  |
| `GEMINI_API_KEY`           | *(empty)*             | Gemini API key - required for AI calls to run   |
| `GEMINI_MODEL`             | `gemini-3.6-flash`    | Primary Gemini model id                         |
| `GEMINI_FALLBACK_MODEL`    | `gemini-3.5-flash`    | Fallback model, retried once on a transient primary failure (429/503/timeout/network) |
| `GEMINI_TIMEOUT_SECONDS`   | `20`                  | Hard per-call timeout                           |
| `GEMINI_MAX_OUTPUT_TOKENS` | `2048`                | Output token cap                                |

With no `GEMINI_API_KEY` set, the application runs normally and every
analysis is created with `aiEnhancementStatus: "DISABLED"` and empty AI
sections - this is the default in local development and in the test
suite (`ai.gemini.enabled: false` in `src/test/resources/application.yml`),
so no test ever makes a live call to Gemini.

## Phase 6 — Recruiter Screening Batch

Phase 6 adds the Recruiter's bulk-upload workflow on top of the frozen
Phase 0–5 pipeline: creating a named batch and bulk-uploading candidate
resumes into it, reusing the Phase 2 parsing/validation logic as-is.
Candidate ranking/matching against a job description is explicitly a
Phase 7 concern and is not implemented here.

### Locked design: no separate Candidate entity

A "candidate" in a screening batch is simply an existing `Resume` row
whose (new, nullable) `screening_batch_id` foreign key points at a
`ScreeningBatch`. There is no `Candidate` table, and the relationship is
one-directional (`Resume` → `ScreeningBatch`, via `Resume.assignToBatch`)
- the same style already used throughout this project to avoid
bidirectional collections and N+1 queries. A Job Seeker's own Phase 2
resume upload is completely unaffected: that foreign key is simply left
null.

### Endpoints

- `POST /api/screening-batches` (`multipart/form-data`) - creates a batch
  and bulk-uploads its resumes in one request. Form fields: `name`
  (required) and one or more `files` parts.
- `GET /api/screening-batches/{id}` - retrieves a batch and its
  associated resumes.

Both are Recruiter-only (a Job Seeker gets `403 Forbidden`) and
ownership-scoped (a Recruiter who does not own the batch gets
`404 Not Found`, identical to a non-existent id, so existence is never
leaked).

### Bulk-upload semantics

Once the request itself passes validation (non-blank name ≤ 255 chars,
at least one file, at most 50 files per request), the batch is always
created, and every file is then parsed independently: a file that fails
(empty, unsupported extension, corrupted, no extractable text - exactly
what `ResumeParsingService` already validates for the single-resume
Phase 2 upload) is reported in the response's `failedFiles` list rather
than aborting the whole batch and discarding every other valid file in
the same request. Nothing is ever silently dropped or mis-associated: a
resume is only ever linked to the batch after it has successfully
parsed, and every file's outcome - success or failure with a reason - is
visible in the response.

`candidateName` for a bulk-uploaded resume defaults to its filename with
the extension stripped (e.g. `alice_resume.pdf` → `alice_resume`), since
the batch overview would otherwise be of little use to a Recruiter.
