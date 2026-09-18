# Phase 1 — Requirements, Architecture & Technology Decisions

**Project:** Cloud-Native AI Incident & Support Management Platform (`incident-platform`)
**Phase:** 1 of 16 — Foundations
**Status:** Approved and built (all 16 phases complete). Kept as the original planning record,
not updated as the build progressed - where reality diverged from the plan (and it did, in
documented, reasoned ways: see [`docs/decisions/`](docs/decisions) for each one), the current
authoritative picture is [`docs/architecture.md`](docs/architecture.md) for the system itself,
[`docs/security.md`](docs/security.md), [`docs/testing.md`](docs/testing.md),
[`docs/api.md`](docs/api.md), and [`docs/deployment.md`](docs/deployment.md) for their respective
areas, and the root [`README.md`](README.md) for the project as it exists today.

---

## 1. Detailed Requirements

### 1.1 Business Context

The project simulates an **internal engineering service desk** — the kind of system a mid-size tech company would use to log, triage, assign, and resolve technical incidents raised by staff. It is deliberately modelled on real tools (Jira Service Management, ServiceNow, PagerDuty, Zendesk) but scoped down to something a single developer can build, run, test, and deploy end-to-end in a portfolio timeframe.

### 1.2 Why This Project

For a UK graduate/junior software engineer CV, generic to-do-list or blog clones are oversaturated and don't demonstrate engineering judgement. An incident management platform is a better vehicle because it naturally requires:

- **Multi-role authorisation logic** (USER vs ENGINEER vs ADMIN) — a real RBAC problem, not just "logged in / not logged in."
- **Stateful domain workflow** (incident status transitions, assignment, escalation) — forces you to think about state machines, concurrency, and data integrity.
- **Cross-service integration** (Java backend calling a Python AI service over REST) — demonstrates microservice thinking without the overhead of a large distributed system.
- **Operational concerns** (metrics, audit logs, dashboards) — shows awareness that software runs in production and needs to be observable.
- **A legitimate, bounded use for AI** (classification/summarisation as an *assist* feature, not the product) — shows you can integrate AI pragmatically rather than chasing hype.

### 1.3 Explicit Non-Goals

To avoid over-engineering (a stated requirement), Phase 1 explicitly rules out:

- Kubernetes/EKS (ECS Fargate is sufficient and far cheaper/simpler for this scale — see §13).
- Event streaming (Kafka, SNS/SQS) — the workflow is request/response; queues would be unjustified complexity.
- Multi-region deployment, blue/green infra, or service mesh.
- Building a custom LLM — the AI service calls a hosted provider through an abstraction layer.
- Notification/email systems — logged as a "future improvement," not built now.

---

## 2. Functional Requirements

Grouped by capability area, each tagged with an ID for traceability into later phases and tests.

**Authentication & Account Management**
- FR-1: A user can register with email, password, and display name.
- FR-2: A user can log in and receive a JWT access token (+ refresh token).
- FR-3: Passwords are stored using a salted hash (BCrypt), never in plain text.
- FR-4: A user's role (USER, ENGINEER, ADMIN) is assigned at creation and only changeable by an ADMIN.

**Incident Management**
- FR-5: A USER can create an incident with title, description, category, and optional attachment metadata.
- FR-6: Any authenticated user can view and search incidents, scoped by role (USERs see their own; ENGINEERs/ADMINs see all, or team-scoped).
- FR-7: An ENGINEER or ADMIN can update incident status (`OPEN → IN_PROGRESS → RESOLVED → CLOSED`, plus `ESCALATED`).
- FR-8: An ENGINEER or ADMIN can assign an incident to an engineer or team.
- FR-9: A USER or ENGINEER can set/change priority and severity (subject to role rules).
- FR-10: Any participant can add comments to an incident.
- FR-11: Every state-changing action on an incident is recorded in an immutable incident history/timeline.
- FR-12: A user can attach reference information (e.g. a filename/URL reference stored as metadata — no binary file storage in v1, noted as a future improvement).

**AI-Assisted Analysis**
- FR-13: On request, the platform sends incident text to the AI service, which returns: suggested category, predicted priority, a short summary, extracted keywords, and suggested troubleshooting steps.
- FR-14: AI analysis is **advisory only** — it never auto-mutates incident data without explicit user/engineer acceptance.
- FR-15: If the AI service is unavailable, incident creation and management continue to function normally (graceful degradation).

**Dashboards & Metrics**
- FR-16: Users see a personal dashboard (their incidents, statuses).
- FR-17: Engineers see an operational dashboard (assigned load, SLA-adjacent aging, resolution stats).
- FR-18: Admins see system-wide metrics (volume by category/severity, team throughput, audit trail).

**Administration**
- FR-19: ADMIN can manage users (view, change role, deactivate).
- FR-20: ADMIN can manage teams and team membership.
- FR-21: ADMIN can manage incident categories.
- FR-22: ADMIN can view an audit log of sensitive actions (role changes, deletions, login events).

---

## 3. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Security** | All endpoints except `/auth/register` and `/auth/login` require a valid JWT. RBAC enforced at the service layer, not just the controller. No secrets in source control. |
| **Performance** | List/search endpoints return in <500ms for realistic seed data volumes (~10k incidents) using proper indexing and pagination. |
| **Reliability** | The core incident workflow (create/view/update) must not depend on the AI service being up. |
| **Scalability** | Stateless backend (JWT, no server-side session) so it can run behind a load balancer with multiple instances. |
| **Maintainability** | Layered architecture, DTOs at boundaries, ≥80% meaningful test coverage on backend service/domain logic. |
| **Observability** | Every service exposes health checks and metrics; logs are structured (JSON) and correlate across services via a request/trace ID. |
| **Portability** | Entire stack runs locally via `docker compose up` with no cloud dependency required for development. |
| **Cost** | Cloud footprint kept within what's realistically affordable for a personal AWS account (see §13) — no always-on expensive managed services beyond a small RDS/ElastiCache instance. |
| **Accessibility/UX** | Frontend usable with keyboard navigation and readable contrast; not a formal WCAG audit, but not ignored either. |

---

## 4. User Roles

| Role | Description | Key Permissions |
|---|---|---|
| **USER** | Any employee raising incidents | Create incidents, view/comment on own incidents, request AI analysis on own incidents, view personal dashboard |
| **ENGINEER** | Support/engineering staff resolving incidents | Everything a USER can do on assigned/team incidents, change status, assign/reassign, escalate, add investigation notes, view operational dashboard |
| **ADMIN** | Platform administrator | Everything an ENGINEER can do, plus manage users/roles/teams/categories, view audit logs and system-wide metrics |

Role hierarchy is **additive**, not purely hierarchical in permissions (an ADMIN isn't just "ENGINEER + more" in every rule — e.g. category management is ADMIN-only regardless), so RBAC is implemented as explicit permission checks per action rather than a simple numeric role level. This is a deliberate design decision worth an ADR (see §16, ADR-002).

---

## 5. Use Cases

A representative (not exhaustive) set, written as actor–goal–flow, to drive both implementation and E2E test design:

**UC-1: Register and log in**
Actor: USER. Goal: obtain access to the platform.
Flow: submit registration → account created with USER role → log in → receive JWT → access protected routes.

**UC-2: Raise an incident**
Actor: USER. Flow: fill incident form (title, description, category) → submit → incident created with status `OPEN` → history entry recorded → (optionally) trigger AI analysis.

**UC-3: Triage and assign**
Actor: ENGINEER/ADMIN. Flow: view incident queue → filter by category/severity/status → open incident → assign to self or another engineer → status moves to `IN_PROGRESS` → history updated.

**UC-4: Investigate and resolve**
Actor: ENGINEER. Flow: add investigation notes/comments → optionally escalate if blocked → update status to `RESOLVED` → USER notified via dashboard state change (not email in v1).

**UC-5: Request AI analysis**
Actor: USER/ENGINEER. Flow: click "Analyse" on an incident → backend calls AI service with incident text → AI service returns classification/summary/keywords/suggested steps → result displayed in an AI panel and stored as an `AIAnalysis` record → user may accept suggested category/priority, which then updates the incident explicitly.

**UC-6: Admin manages a team**
Actor: ADMIN. Flow: create team → add/remove engineers → reassign incidents in bulk if a team is deactivated.

**UC-7: View metrics**
Actor: any role. Flow: open dashboard → see role-appropriate charts (personal vs operational vs system-wide) sourced from aggregate queries, not client-side computation over full datasets.

---

## 6. System Architecture

### 6.1 High-Level Component Diagram

```
                     ┌─────────────────────────┐
                     │   React + TypeScript     │
                     │   (Vite, TanStack Query) │
                     └────────────┬─────────────┘
                                  │ HTTPS / JSON (JWT bearer)
                                  ▼
                     ┌─────────────────────────┐
                     │   Spring Boot REST API   │
                     │   (Java 21, layered      │
                     │    architecture)          │
                     └──────┬─────────────┬─────┘
                            │             │
                  ┌─────────▼───┐   ┌─────▼──────┐
                  │ PostgreSQL  │   │   Redis    │
                  │ (system of  │   │ (cache /   │
                  │  record)    │   │  rate limit)│
                  └─────────────┘   └────────────┘
                            │
                            │ REST (internal, service-to-service JWT)
                            ▼
                  ┌───────────────────────┐
                  │  FastAPI AI Service    │
                  │  (Python 3.12)         │
                  └───────────┬────────────┘
                              │
                              ▼
                  ┌───────────────────────┐
                  │   LLM Provider (via    │
                  │   abstraction layer)   │
                  └───────────────────────┘
```

### 6.2 Backend Layering (Clean Architecture)

```
Controller  → handles HTTP, request/response DTO mapping, validation
    ↓
Service     → business logic, orchestration, transaction boundaries
    ↓
Domain      → entities, value objects, domain rules (framework-agnostic where practical)
    ↓
Repository  → Spring Data JPA interfaces, persistence concerns only
    ↓
Database    → PostgreSQL
```

Rules enforced across the codebase:
- Controllers never touch repositories directly.
- Domain entities are never returned from controllers — DTOs only, mapped explicitly (MapStruct or manual mappers, decided in Phase 4).
- Cross-cutting concerns (auth, logging, exception handling) live in dedicated layers (filters/advice), not scattered through business logic.

### 6.3 Why a Separate AI Microservice (Not a Library Inside the Java App)

- **Language fit**: Python has the strongest ecosystem for LLM tooling and rapid iteration; forcing this into Java would fight the ecosystem.
- **Failure isolation**: FR-15 requires the core platform to survive AI outages. A separate process with its own deploy/restart lifecycle makes that isolation structural, not just a try/catch.
- **Independent scaling**: AI calls are latency-bound and bursty; it can scale (or be paused) independently of the core API.
- **Demonstrates microservice skills**: A genuine second service with its own API contract, tests, and deployment pipeline — not a simulated one.

The tradeoff (extra network hop, two codebases to run locally) is accepted and mitigated by Docker Compose and a small, clearly versioned OpenAPI contract between the two services.

---

## 7. Technology Justification

| Choice | Why |
|---|---|
| **Java 21 + Spring Boot 3** | Dominant in UK enterprise backend roles; Spring Security + Spring Data give production-grade auth and persistence patterns for free; Java 21 (LTS) shows currency with modern language features (records, virtual threads where relevant). |
| **PostgreSQL** | Relational integrity matters here (incidents, users, teams, history are highly relational); widely used in industry; strong AWS RDS support. |
| **Redis** | Used narrowly and honestly: caching hot dashboard queries and backing rate limiting — not forced in everywhere. |
| **Flyway** | Version-controlled schema migrations are a baseline professional expectation; also documents schema evolution for the README/ADRs. |
| **FastAPI (Python)** | Async-native, automatic OpenAPI generation, Pydantic validation — the modern standard for Python REST services and a good pairing with an LLM abstraction layer. |
| **React + TypeScript + Vite + TanStack Query** | Current industry-standard frontend stack; TanStack Query specifically demonstrates understanding of server-state caching vs client-state, a common interview topic. |
| **Docker + Docker Compose** | Reproducible local dev, and the same images are promoted to ECR/ECS — "build once, run everywhere" is demonstrated rather than asserted. |
| **Terraform** | Industry-standard IaC; module structure demonstrates reusability thinking, a key differentiator from "I clicked buttons in the AWS console." |
| **AWS ECS/Fargate (not EKS)** | Fargate gives real container orchestration, IAM task roles, and ALB integration without the cost and operational overhead of running/managing a Kubernetes control plane — the right complexity level for a graduate project (see §13). |
| **GitHub Actions** | Free for public repos, tightly integrated with the repo, and the most commonly seen CI tool in junior job postings alongside GitLab CI. |
| **Testcontainers** | Lets backend integration tests run against a real PostgreSQL in CI, not a mocked/in-memory substitute — a detail interviewers specifically probe for. |
| **Playwright** | Modern, reliable E2E framework with good TypeScript support, replacing the older Selenium approach. |
| **Prometheus + Grafana** | Standard, free, self-hostable observability stack; pairs naturally with Spring Boot Actuator's `/actuator/prometheus` endpoint. |

---

## 8. Database Design

### 8.1 Core Entities (conceptual, refined with exact columns in Phase 3)

- **User**: id, email, password_hash, display_name, role, team_id (nullable), is_active, created_at, updated_at
- **Role**: id, name (`USER`, `ENGINEER`, `ADMIN`) — modelled as an enum column in v1; a separate table is a documented option if role-permission granularity grows (see ADR-003)
- **Team**: id, name, description, created_at
- **IncidentCategory**: id, name, description, is_active
- **Incident**: id, title, description, category_id, status, priority, severity, created_by_id, assigned_to_id (nullable), team_id (nullable), created_at, updated_at
- **IncidentComment**: id, incident_id, author_id, body, created_at
- **IncidentAssignment**: id, incident_id, assigned_to_id, assigned_by_id, assigned_at, unassigned_at (nullable) — history of assignment changes, not just current state
- **IncidentHistory**: id, incident_id, actor_id, field_changed, old_value, new_value, changed_at — generic audit trail for the incident timeline UI
- **AuditLog**: id, actor_id, action, entity_type, entity_id, metadata (JSONB), created_at — platform-wide sensitive-action log (role changes, deletions, logins)
- **AIAnalysis**: id, incident_id, suggested_category, predicted_priority, summary, keywords (JSONB/array), suggested_steps (JSONB/array), model_used, created_at

### 8.2 Design Principles

- Every table has `created_at`/`updated_at`; audit-relevant tables are append-only by design (no update/delete on `IncidentHistory` or `AuditLog` rows).
- Foreign keys enforced at the database level, not just application level.
- Indexes planned on: `incident.status`, `incident.assigned_to_id`, `incident.category_id`, `incident.created_at` (for search/filter/sort performance per NFR).
- `IncidentAssignment` and `IncidentHistory` are kept separate: assignment is a specific, queryable relationship (who owns this now), while history is the general-purpose timeline feed. Conflating them would force awkward queries later.
- Schema changes are only ever made through Flyway migrations, never manual DDL — enforced as a team rule from Phase 3 onward.
- A full ER diagram (generated from the actual schema, not hand-drawn) will be produced in Phase 3 and placed in `/docs/architecture.md`.

---

## 9. API Design

### 9.1 Conventions

- Versioned under `/api/v1`.
- JSON request/response bodies; DTOs, never JPA entities, cross the wire.
- Standard HTTP status codes (`200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `422`, `500`) used consistently, backed by a global exception handler (`@ControllerAdvice`) returning a consistent error envelope (`{ "error": { "code", "message", "details" } }`).
- List endpoints support `page`, `size`, `sort`, and relevant filter query params; responses use a standard paged envelope (`content`, `page`, `size`, `totalElements`, `totalPages`).
- OpenAPI 3 spec generated from code (springdoc-openapi for Java, FastAPI's native OpenAPI for Python) — not hand-maintained separately, so it can't drift.

### 9.2 Representative Endpoints

```
Auth
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh

Incidents
GET    /api/v1/incidents               ?status=&priority=&category=&assignedTo=&page=&size=&sort=
POST   /api/v1/incidents
GET    /api/v1/incidents/{id}
PUT    /api/v1/incidents/{id}
DELETE /api/v1/incidents/{id}
GET    /api/v1/incidents/{id}/history
POST   /api/v1/incidents/{id}/comments
POST   /api/v1/incidents/{id}/assign
POST   /api/v1/incidents/{id}/escalate
POST   /api/v1/incidents/{id}/ai-analysis
GET    /api/v1/incidents/{id}/ai-analysis

Dashboard
GET    /api/v1/dashboard/metrics        (role-scoped response)

Users / Teams / Categories (admin-facing, RBAC-guarded)
GET    /api/v1/users
PATCH  /api/v1/users/{id}/role
GET    /api/v1/teams
POST   /api/v1/teams
GET    /api/v1/categories
POST   /api/v1/categories
GET    /api/v1/audit-logs

AI Service (internal, called by backend, not directly by frontend)
POST   /internal/v1/analyse             { title, description, category? } → { suggestedCategory, predictedPriority, summary, keywords[], suggestedSteps[] }
GET    /internal/v1/health
```

The AI service is deliberately **not exposed to the frontend directly** — the Java backend proxies the call, adds auth/authorisation, persists the `AIAnalysis` record, and returns a shaped response. This keeps a single authentication boundary and matches how this would realistically be done in an enterprise setting.

---

## 10. Security Architecture

- **Authentication**: JWT (access + refresh token pair) issued by the Spring Boot backend on login. Access tokens short-lived (~15 min); refresh tokens longer-lived and rotated.
- **Authorisation**: Method-level RBAC using Spring Security (`@PreAuthorize`) *in addition to* explicit service-layer checks for ownership-based rules (e.g. "a USER can only view their own incidents") that role annotations alone can't express.
- **Password storage**: BCrypt with a sufficient work factor; never logged, never returned in any DTO.
- **Transport**: HTTPS enforced in all non-local environments (TLS termination at the ALB in AWS).
- **Input validation**: Bean Validation (`jakarta.validation`) on all request DTOs; FastAPI/Pydantic validation on the AI service side.
- **CORS**: Explicit allow-list of the frontend origin(s), not a wildcard.
- **Rate limiting**: Redis-backed, applied to auth endpoints first (`/auth/login`, `/auth/register`) to reduce brute-force/credential-stuffing risk.
- **Secrets management**: No secrets in Git, ever. Local dev uses `.env` files (git-ignored, with a committed `.env.example`); AWS uses Secrets Manager, injected into ECS tasks as environment variables at runtime, never baked into images.
- **Service-to-service auth**: The internal call from the Java backend to the AI service is authenticated with a shared internal token/header (not left open), even though it's not internet-facing, to model realistic defence-in-depth.
- **Audit logging**: Sensitive actions (login, role change, incident deletion, user deactivation) written to `AuditLog`, viewable only by ADMIN.
- **Dependency/image scanning**: Covered in CI/CD (§12) — Dependabot/OWASP dependency-check for code, Trivy (or equivalent) for Docker images.

A dedicated `/docs/security.md` and threat-model-style write-up is scheduled for Phase 15, but the architecture above is designed in from Phase 1 rather than retrofitted.

---

## 11. Testing Strategy

| Layer | Tooling | What it covers |
|---|---|---|
| Backend unit | JUnit 5, Mockito | Service logic, domain rules, mappers, validators — in isolation, no Spring context |
| Backend slice/controller | Spring Boot Test (`@WebMvcTest`) | Request validation, status codes, RBAC annotations, error envelope shape |
| Backend integration | Spring Boot Test (`@SpringBootTest`) + Testcontainers PostgreSQL | Repository queries, transactional behaviour, real schema via Flyway |
| Backend security | Spring Security Test | Access is correctly denied/allowed per role per endpoint |
| AI service unit/API | Pytest, FastAPI TestClient | Request validation, response shaping, provider abstraction logic |
| AI service provider mocking | Pytest + mocked HTTP layer | LLM calls mocked in CI — no real API cost/flakiness in the pipeline |
| Frontend component | Vitest + React Testing Library | Forms, incident list rendering, role-conditional UI |
| Frontend API integration | MSW (Mock Service Worker) + TanStack Query | Loading/error/success states against a mocked API layer |
| End-to-end | Playwright | Full user journeys across the running stack (see UC-1 to UC-7) run against Docker Compose in CI |

Coverage target: meaningful coverage of service/domain logic (~80%+), not a coverage-percentage chase on trivial getters/setters — this distinction will be stated explicitly in `/docs/testing.md` so it reads as a considered decision, not a number picked for its own sake.

---

## 12. CI/CD Strategy

**Pull Request pipeline (`.github/workflows/pr.yml`)**
1. Checkout
2. Set up Java/Node/Python toolchains (cached)
3. Lint/format check: Checkstyle/Spotless (Java), ESLint/Prettier (TypeScript), Ruff/Black (Python)
4. Backend unit + slice tests
5. Backend integration tests (Testcontainers, needs Docker-in-Docker on the runner)
6. AI service unit/API tests
7. Frontend unit/component tests
8. Static analysis (SonarQube/SonarCloud)
9. Dependency vulnerability scan (OWASP Dependency-Check / `npm audit` / `pip-audit`)
10. Playwright E2E against a Docker Compose stack spun up in the runner
11. Fail fast, required status checks block merge

**Main branch pipeline (`.github/workflows/deploy.yml`)**
1. Re-run build (don't trust PR artifacts blindly)
2. Build Docker images for backend, AI service, frontend
3. Scan images (Trivy) — block on high/critical CVEs
4. Push to Amazon ECR
5. `terraform plan` (and `apply` behind a manual approval gate, at least initially — matches real-world practice of not blindly auto-applying infra changes)
6. Deploy new task definitions to ECS
7. Run smoke tests against the deployed environment
8. Post deployment status (GitHub deployment status / Slack-style summary, kept simple)

This two-pipeline split (PR vs main) is itself a deliberate decision worth an ADR — it demonstrates understanding that verification and deployment are different concerns with different trust levels.

---

## 13. AWS Architecture

Chosen for the best balance of **production realism, complexity, and personal-account affordability**:

- **Amazon ECS on Fargate** — runs the backend, AI service, and (optionally) a static-hosted or containerised frontend. No EC2 instances to patch, no EKS control-plane cost (~$73/month alone), and still demonstrates real container orchestration, task definitions, and IAM task roles.
- **Application Load Balancer (ALB)** — path-based routing to backend vs AI service if needed, TLS termination, health-check integration with ECS.
- **Amazon RDS (PostgreSQL, `db.t4g.micro` or similar)** — managed database, automated backups; sized deliberately small since this is a portfolio project, not a production workload.
- **Amazon ElastiCache (Redis, smallest node type)** — caching/rate-limiting backend.
- **Amazon S3** — used narrowly (e.g. static frontend hosting if not containerised, or future file-attachment storage) — not introduced just for the sake of it.
- **Amazon ECR** — private image registry for all three service images.
- **AWS Secrets Manager** — database credentials, JWT signing key, LLM API key.
- **IAM** — least-privilege task roles per service (the AI service's task role, for example, has no database access at all — it never talks to Postgres directly).
- **CloudWatch** — logs and base metrics; Prometheus/Grafana (self-hosted, e.g. on a small container or `docker compose`-style setup) layered on top for the richer dashboards described in the observability requirement, avoiding the cost of AWS Managed Prometheus/Grafana for a portfolio project.

Explicitly **not used**, with reasoning captured in an ADR: EKS (cost/complexity), Lambda (the workload is a long-running stateful API, a poor serverless fit), multi-AZ RDS (cost, not needed to demonstrate the skill), CloudFront (no meaningful CDN need at this scale).

A cost-awareness note will be included in `/docs/deployment.md`: how to tear down infrastructure (`terraform destroy`) between demo sessions to avoid ongoing AWS charges — itself a realistic and interview-relevant practice.

---

## 14. Terraform Strategy

Structured as reusable modules under `infrastructure/terraform/`:

```
infrastructure/terraform/
├── environments/
│   └── dev/            # root module wiring everything together for one environment
│       ├── main.tf
│       ├── variables.tf
│       └── outputs.tf
└── modules/
    ├── networking/      # VPC, subnets, route tables, security groups
    ├── iam/             # task roles, execution roles, policies
    ├── database/        # RDS instance, subnet group, parameter group
    ├── cache/            # ElastiCache Redis
    ├── ecs/             # cluster, task definitions, services
    ├── load-balancer/   # ALB, target groups, listeners
    ├── secrets/         # Secrets Manager entries
    └── monitoring/      # CloudWatch log groups, alarms
```

Principles: remote state (S3 backend + DynamoDB lock table, itself provisioned by a small bootstrap config), environment separation via a root module per environment rather than workspaces (clearer for a solo/portfolio context and easier to explain in an interview), variables for anything environment-specific, no hard-coded account IDs/ARNs in module code.

---

## 15. Repository Structure

As specified, confirmed for Phase 1:

```
incident-platform/
├── backend/            # Java 21 / Spring Boot 3
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── ai-service/         # Python 3.12 / FastAPI
│   ├── app/
│   ├── tests/
│   ├── requirements.txt
│   └── Dockerfile
├── frontend/            # React / TypeScript / Vite
│   ├── src/
│   ├── tests/
│   ├── package.json
│   └── Dockerfile
├── infrastructure/
│   └── terraform/
├── tests/
│   └── e2e/             # Playwright, cross-service
├── .github/
│   └── workflows/
├── docs/
│   ├── architecture.md
│   ├── api.md
│   ├── testing.md
│   ├── deployment.md
│   ├── security.md
│   └── decisions/       # ADRs
├── docker-compose.yml
├── README.md
└── .gitignore
```

Git workflow (detailed in Phase 2): trunk-based with short-lived feature branches, Conventional Commits, PR template requiring linked phase/issue, required status checks before merge, no direct pushes to `main`.

---

## 16. Development Roadmap

Restating the 16-phase plan as the working roadmap, each phase producing runnable, tested, documented output before moving on:

1. **Requirements & architecture** *(this document)*
2. Repository setup & development standards (branching, linting configs, editor config, PR/issue templates)
3. Database schema & Flyway migrations
4. Spring Boot backend skeleton (layered architecture, DTOs, exception handling)
5. Authentication & RBAC
6. Incident management functionality (core domain)
7. React/TypeScript frontend
8. Python AI microservice
9. Testing hardening across all layers
10. Dockerisation (all services + Compose)
11. CI/CD with GitHub Actions
12. Terraform & AWS infrastructure
13. Cloud deployment
14. Observability & monitoring
15. Security hardening
16. Documentation & final polish

Each phase's deliverable will include: an explanation of what's being built and why, production-quality code (no placeholders/fake functionality), tests written alongside implementation, updated docs, and a logically organised set of commits.

**First Architecture Decision Records to write in Phase 2** (flagged during this phase):
- ADR-001: Separate AI microservice vs embedded AI logic
- ADR-002: Explicit per-action RBAC checks vs hierarchical role levels
- ADR-003: Role as an enum column vs a normalised roles/permissions table
- ADR-004: ECS Fargate vs EKS
- ADR-005: Two-pipeline CI/CD split (PR verification vs main deployment)

---

## 17. Definition of Done (per phase, and for the project overall)

A phase is **not done** unless:

- [ ] Code builds and runs (`docker compose up` still works end-to-end after the phase's changes)
- [ ] Tests for the new functionality exist and pass in CI
- [ ] No placeholder/stubbed logic left where real logic was requested
- [ ] Linting/formatting passes
- [ ] Relevant documentation (README section and/or `/docs/*.md`) is updated in the same phase, not deferred
- [ ] Commits are logically scoped and use Conventional Commit messages
- [ ] Any non-obvious design decision is captured as an ADR or inline rationale
- [ ] The change has been explained (what + why) before moving to the next phase

The **project overall** is done when all 16 phases are complete, the application is deployed and reachable on AWS, the full CI/CD pipeline runs green on a fresh clone, and the README/docs are sufficient for a stranger (or interviewer) to understand, run, and reason about the system without further explanation from you.

---

## Next Step

This document is the Phase 1 deliverable. Once you've reviewed and approved it (or asked for changes), Phase 2 will set up the actual repository: folder scaffolding, `.gitignore`, linting/formatting configs for all three services, branch protection conventions, and PR/issue templates — the groundwork everything else builds on.
