# Cloud-Native AI Incident & Support Management Platform

An enterprise-style internal engineering service desk: users raise incidents, engineers triage and resolve them, admins manage the platform — with an optional AI-assisted analysis service (classification, summarisation, keyword extraction, suggested troubleshooting steps) sitting alongside the core workflow, not at the centre of it.

> **Status: Phase 6 of 16 — incident management functionality.**
> The application does not yet do anything. See [`docs/decisions/`](docs/decisions) for the reasoning behind key choices and the phase-by-phase build log in commit history.

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
```

## Roadmap

1. ✅ Requirements & architecture
2. ✅ Repository setup & development standards
3. ✅ Database schema & Flyway migrations
4. ✅ Spring Boot backend skeleton
5. ✅ Authentication & RBAC
6. ✅ Incident management functionality *(this phase)*
7. ⬜ React/TypeScript frontend
8. ⬜ Python AI microservice
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
