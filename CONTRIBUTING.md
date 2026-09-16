# Contributing / Development Standards

This is a solo portfolio project, but it follows the same conventions a real engineering team would enforce — partly because it's good practice, and partly because being able to explain *why* a convention exists is exactly what a graduate interview probes for.

## Branching strategy

**Trunk-based development with short-lived feature branches.**

- `main` is always deployable — protected, no direct pushes, merges only via PR with required status checks green.
- Branch naming: `phase-<n>/<short-description>`, e.g. `phase-3/incident-schema-migrations`, `phase-5/jwt-authentication`.
- Branches are kept short-lived (days, not weeks) and scoped to one phase's slice of work — large phases are split into multiple PRs rather than one giant branch.
- Rebase onto `main` before opening a PR; avoid merge commits cluttering history — keep it linear and readable.

Why trunk-based over GitFlow: this project has no long-running release branches or parallel version support to manage, so GitFlow's extra ceremony (develop/release/hotfix branches) would be complexity without a corresponding benefit — a judgement call worth being able to explain, not just a default.

## Commit conventions

[Conventional Commits](https://www.conventionalcommits.org/), enforced by convention (not currently tooling-enforced — commit hooks can be added later if it proves useful):

```
<type>(<scope>): <short summary>

[optional body]

[optional footer]
```

Types used in this project: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `build`, `perf`.

Examples:
```
feat(backend): add incident status transition validation
test(ai-service): add unit tests for keyword extraction
docs(architecture): add database ER diagram
ci(pipeline): add Trivy image scanning to deploy workflow
```

Scope is typically the service (`backend`, `ai-service`, `frontend`, `infra`, `ci`, `docs`) or a specific subsystem where useful.

## Pull requests

- One PR per logical unit of work; large phases are broken into several PRs rather than shipped as one.
- PR description follows [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) — what changed, why, how it was tested, and a Definition of Done checklist.
- All required CI checks must pass before merge (once CI exists from Phase 11 onward; before that, checks are manual — see below).
- No self-approval bypass on protected settings; for a solo project this is enforced by discipline (actually running the DoD checklist) rather than a second reviewer.

## Branch protection (GitHub settings, configured on `main`)

- Require a pull request before merging (no direct pushes).
- Require status checks to pass before merging (from Phase 11 onward: lint, unit tests, integration tests).
- Require branches to be up to date before merging.
- Require linear history (no merge commits).

## Code style & linting

| Service | Formatter/Linter | Config |
|---|---|---|
| Backend (Java) | Spotless (Google Java Format) | `backend/pom.xml` |
| AI service (Python) | Ruff (lint) + Black (format) | `ai-service/pyproject.toml` |
| Frontend (TypeScript) | ESLint + Prettier | `frontend/.eslintrc.cjs`, `frontend/.prettierrc` |

Run before committing:

```bash
# Backend
cd backend && mvn spotless:apply

# AI service
cd ai-service && ruff check --fix . && black .

# Frontend
cd frontend && npm run lint -- --fix && npm run format
```

From Phase 11, these run automatically in CI on every PR and will block merge on failure.

## Definition of Done (per phase)

Copied from [`PHASE-1-requirements-and-architecture.md`](PHASE-1-requirements-and-architecture.md) §17 — every phase must satisfy this before being considered complete:

- [ ] Code builds and runs (`docker compose up` still works end-to-end after the phase's changes)
- [ ] Tests for the new functionality exist and pass
- [ ] No placeholder/stubbed logic left where real logic was requested
- [ ] Linting/formatting passes
- [ ] Relevant documentation is updated in the same phase, not deferred
- [ ] Commits are logically scoped and use Conventional Commit messages
- [ ] Any non-obvious design decision is captured as an ADR or inline rationale
- [ ] The change has been explained (what + why) before moving to the next phase

## Architecture Decision Records

Significant, hard-to-reverse decisions are documented in [`docs/decisions/`](docs/decisions) using the template in [`docs/decisions/template.md`](docs/decisions/template.md). Write one whenever you catch yourself thinking "we could do this several reasonable ways" — that's the signal a decision needs a record, not just a comment in code.
