# Cloud-Native AI Incident & Support Management Platform

An enterprise-style internal engineering service desk: users raise incidents, engineers triage and resolve them, admins manage the platform — with an optional AI-assisted analysis service (classification, summarisation, keyword extraction, suggested troubleshooting steps) sitting alongside the core workflow, not at the centre of it.

> **Status: Phase 2 of 16 — repository scaffolding and development standards.**
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

## Development standards

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for branching strategy, commit conventions, linting/formatting, and the per-phase Definition of Done.

## Architecture decisions

Non-obvious design choices are recorded as Architecture Decision Records in [`docs/decisions/`](docs/decisions), starting with:

- [ADR-0001](docs/decisions/0001-separate-ai-microservice.md) — Separate AI microservice vs embedded AI logic
- [ADR-0002](docs/decisions/0002-explicit-per-action-rbac.md) — Explicit per-action RBAC vs hierarchical role levels
- [ADR-0003](docs/decisions/0003-role-as-enum-column.md) — Role as an enum column vs a normalised roles/permissions table
- [ADR-0004](docs/decisions/0004-ecs-fargate-over-eks.md) — ECS Fargate over EKS
- [ADR-0005](docs/decisions/0005-two-pipeline-ci-cd.md) — Two-pipeline CI/CD split (PR verification vs main deployment)

## Roadmap

1. ✅ Requirements & architecture
2. ✅ Repository setup & development standards *(this phase)*
3. ⬜ Database schema & Flyway migrations
4. ⬜ Spring Boot backend skeleton
5. ⬜ Authentication & RBAC
6. ⬜ Incident management functionality
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
