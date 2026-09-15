# Cloud-Native AI Incident & Support Management Platform

An enterprise-style internal engineering service desk: users raise incidents, engineers triage and resolve them, admins manage the platform — with an optional AI-assisted analysis service (classification, summarisation, keyword extraction, suggested troubleshooting steps) sitting alongside the core workflow, not at the centre of it.

> **Status: Phase 8 of 16 — Python AI microservice.**
> Core auth/RBAC, incident management, dashboards, and the frontend are built and runnable (Phases 3-7); the AI service now runs independently too, but isn't yet wired end-to-end into the backend. See [`docs/decisions/`](docs/decisions) for the reasoning behind key choices and the phase-by-phase build log in commit history.

## Why this project exists

Built as a portfolio project to demonstrate production-oriented software engineering practice for UK graduate/junior engineering roles: clean layered architecture, a real (small) microservice split, infrastructure as code, CI/CD, automated testing at every layer, and observability — with AI integrated as a supporting feature rather than the whole point.

## Technology stack

| Layer | Stack |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL, Redis, Flyway, Maven |
| AI service | Python 3.12, FastAPI, Pydantic, Pytest |
| Frontend | React, TypeScript, Vite, React Router, TanStack Query |
| Testing | JUnit 5, Mockito, Testcontainers, Pytest, Vitest, Playwright |
| Infrastructure | Docker, Docker Compose, Terraform, AWS (ECS Fargate, RDS, ElastiCache, ALB, ECR, Secrets Manager) |
| CI/CD | GitHub Actions |
| Observability | Spring Boot Actuator, Prometheus, Grafana, structured logging |

Full technology justification: see [`PHASE-1-requirements-and-architecture.md`](PHASE-1-requirements-and-architecture.md) (superseded by `docs/architecture.md` once that's populated in a later phase).

## Repository structure

```
incident-platform/
├── backend/            # Java 21 / Spring Boot 3 REST API
├── ai-service/         # Python 3.12 / FastAPI AI microservice
├── frontend/            # React / TypeScript / Vite dashboard
├── infrastructure/
│   └── terraform/       # IaC, module-per-concern
├── tests/
│   └── e2e/              # Playwright, cross-service end-to-end tests
├── docs/                 # Architecture, API, testing, deployment, security docs + ADRs
├── .github/              # CI workflows, PR/issue templates
├── docker-compose.yml    # Local development environment
└── .env.example          # Documented environment variables (no real secrets)
```

## Local development

At this phase, only the infrastructure dependencies are runnable:

```bash
cp .env.example .env
docker compose up postgres redis
```

Application services (`backend`, `ai-service`, `frontend`) are added to `docker-compose.yml` once their Dockerfiles exist (Phase 10). Instructions for running each service individually will be added as they're built.

The database schema (`backend/src/main/resources/db/migration/`) isn't applied automatically yet — that happens when the backend boots and Flyway runs on startup (Phase 4 onward). The migrations themselves are verified independently by `FlywayMigrationTest` (Testcontainers) and were manually run end-to-end against PostgreSQL 16 during Phase 3.

## Development standards

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for branching strategy, commit conventions, linting/formatting, and the per-phase Definition of Done.

## Architecture decisions

Non-obvious design choices are recorded as Architecture Decision Records in [`docs/decisions/`](docs/decisions), starting with:

- [ADR-0001](docs/decisions/0001-separate-ai-microservice.md) — Separate AI microservice vs embedded AI logic
- [ADR-0002](docs/decisions/0002-explicit-per-action-rbac.md) — Explicit per-action RBAC vs hierarchical role levels
- [ADR-0003](docs/decisions/0003-role-as-enum-column.md) — Role as an enum column vs a normalised roles/permissions table
- [ADR-0004](docs/decisions/0004-ecs-fargate-over-eks.md) — ECS Fargate over EKS
- [ADR-0005](docs/decisions/0005-two-pipeline-ci-cd.md) — Two-pipeline CI/CD split (PR verification vs main deployment)
- [ADR-0006](docs/decisions/0006-check-constraints-over-native-enums.md) — CHECK constraints over native Postgres enum types
- [ADR-0007](docs/decisions/0007-manual-mappers-over-mapstruct.md) — Manual mapper methods over MapStruct
- [ADR-0008](docs/decisions/0008-jwt-refresh-token-strategy.md) — Opaque Redis-backed refresh tokens + manual credential verification
- [ADR-0009](docs/decisions/0009-admin-bootstrap-strategy.md) — Idempotent startup runner for the first ADMIN account
- [ADR-0010](docs/decisions/0010-frontend-token-storage.md) — Refresh token in localStorage, access token in memory only
- [ADR-0011](docs/decisions/0011-pluggable-analysis-provider.md) — Runtime-selectable analysis provider (mock default, real LLM optional)

## Running the backend locally

Requires Maven and JDK 21 installed locally (`mvn -version` should show Java 21).

```bash
docker compose up -d postgres redis
cd backend
mvn spring-boot:run
```

The app boots on `:8080`. Flyway applies all migrations automatically on startup. If
`ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD` are set in your `.env` (they are by default in
`.env.example`), an ADMIN account is created automatically on first startup — see ADR-0009.

```bash
# Register a regular USER
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123","displayName":"Your Name"}'

# Log in as the bootstrapped admin (see .env for the credentials)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"change-me-admin-password"}'

# Create an incident (any authenticated user)
curl -X POST http://localhost:8080/api/v1/incidents \
  -H "Authorization: Bearer <accessToken>" -H "Content-Type: application/json" \
  -d '{"title":"Laptop wont boot","description":"Black screen","categoryId":1}'

# List/search incidents, view dashboard metrics, etc.
curl "http://localhost:8080/api/v1/incidents?status=OPEN" -H "Authorization: Bearer <accessToken>"
curl http://localhost:8080/api/v1/dashboard/metrics -H "Authorization: Bearer <accessToken>"

# Request AI analysis for an incident (requires ai-service running — see below)
curl -X POST http://localhost:8080/api/v1/incidents/1/ai-analysis -H "Authorization: Bearer <accessToken>"
curl http://localhost:8080/api/v1/incidents/1/ai-analysis -H "Authorization: Bearer <accessToken>"
```

## Running the frontend locally

Requires Node.js 18+ (`node --version`).

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Opens on `:5173` and talks to the backend on `:8080` (must be running — see above). Log in with
the bootstrapped admin credentials, or register a new account, and use the app.

```bash
npm run build        # type-checks (tsc -b) then produces a production bundle
npm run test          # Vitest — includes MSW-mocked tests of the token refresh/retry logic
npm run lint           # ESLint
npm run format:check   # Prettier
```

## Running the AI service locally

Requires Python 3.12+ (a slightly older 3.11 also works for local dev/testing).

```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Defaults to a deterministic mock analysis provider — no API key needed (see
[ADR-0011](docs/decisions/0011-pluggable-analysis-provider.md)):

```bash
curl http://localhost:8000/internal/v1/health

curl -X POST http://localhost:8000/internal/v1/analyse \
  -H "Content-Type: application/json" \
  -H "X-Internal-Token: change-me-shared-internal-token" \
  -d '{"title":"VPN keeps disconnecting","description":"My VPN drops every few minutes and I cannot reach internal services."}'
```

To use a real hosted model instead, set `LLM_PROVIDER=openai`,
`LLM_API_KEY=...`, and `LLM_MODEL=...` in your `.env` before starting the
service. The Spring Boot backend now calls this end-to-end via
`POST`/`GET /api/v1/incidents/{id}/ai-analysis` (see `com.incidentplatform.ai`
and `docs/architecture.md`).

```bash
ruff check .           # lint
black --check .        # format check
mypy app                # type check
pytest                  # unit + API tests (mock provider only — no network/API key needed)
```

## Roadmap

1. ✅ Requirements & architecture
2. ✅ Repository setup & development standards
3. ✅ Database schema & Flyway migrations
4. ✅ Spring Boot backend skeleton
5. ✅ Authentication & RBAC
6. ✅ Incident management functionality
7. ✅ React/TypeScript frontend
8. ✅ Python AI microservice *(this phase)*
9. ⬜ Testing hardening
10. ⬜ Dockerisation
11. ⬜ CI/CD with GitHub Actions
12. ⬜ Terraform & AWS infrastructure
13. ⬜ Cloud deployment
14. ⬜ Observability & monitoring
15. ⬜ Security hardening
16. ⬜ Documentation & final polish

## License

MIT — see [`LICENSE`](LICENSE).
