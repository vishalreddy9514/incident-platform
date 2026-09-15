# Testing Strategy

Full strategy as originally planned: see
[`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md) §11.
This document tracks what's actually built, updated through Phase 9.

## Coverage target

"Meaningful coverage of service/domain logic, not a coverage-percentage chase on trivial
getters/setters" (Phase 1 §11) — each service has a coverage *reporter* configured, but no
minimum-ratio gate that fails the build on a number alone:

| Service | Tool | Command |
|---|---|---|
| Backend | JaCoCo | `mvn test` → `target/site/jacoco/index.html` |
| AI service | pytest-cov | `pytest --cov=app --cov-report=term-missing` |
| Frontend | @vitest/coverage-v8 | `npm run test:coverage` → `coverage/index.html` |

## Current state (Phase 9)

### Backend

94 of 99 tests pass in an environment without a Docker daemon (this repo's own sandbox
included) — the other 5 need Testcontainers: `FlywayMigrationTest`,
`IncidentPlatformApplicationTests`, `AuthenticationIntegrationTest`,
`IncidentManagementIntegrationTest`, `IncidentCategoryRepositoryTest`. Line coverage on the
runnable 94: **~68%**.

Phase 9 added:
- Controller slice tests that didn't exist at all: `TeamControllerTest`, `UserControllerTest`,
  `DashboardControllerTest`.
- The five `IncidentStatus` transition rejections `IncidentServiceTest` didn't cover
  (`OPEN→RESOLVED`, `ESCALATED→OPEN`, `ESCALATED→CLOSED`, `RESOLVED→OPEN`,
  `RESOLVED→ESCALATED`), as a parameterised test.
- Mapper unit tests that didn't exist: `IncidentMapperTest`, `CategoryMapperTest`,
  `TeamMapperTest`, `UserMapperTest`, `AiAnalysisMapperTest`.
- `AdminBootstrapRunnerTest` — previously zero coverage on a class every ADMIN-only endpoint
  transitively depends on being correct.
- RBAC-denial coverage in `AuthenticationIntegrationTest` for `POST /categories`,
  `PUT /categories/{id}`, `POST /teams`, and `PATCH /users/{id}/role` — the admin-gated
  endpoints that had none. **Not runnable in this sandbox** (Testcontainers/Docker) — written
  and reviewed carefully, but unverified here; should be the first thing checked in an
  environment with Docker access.

**A real finding along the way**: `@PreAuthorize` does **not** apply inside a `@WebMvcTest`
slice with `@AutoConfigureMockMvc(addFilters = false)` — verified empirically (a non-admin role
still reached an ADMIN-only endpoint in a slice test). `addFilters = false` skips the servlet
filter chain entirely, and Spring's method-security AOP interceptor isn't active in that reduced
context either. This means RBAC denial genuinely cannot be tested at the slice level here — it
has to be an integration test with the real `SecurityConfig` loaded, which is why the new
RBAC-denial tests above live in `AuthenticationIntegrationTest`, not a slice test. (A related,
separate finding: `@WebMvcTest` *does* still construct `@Component`-annotated `Filter` beans
despite `addFilters = false` — that one's from Phase 8's test-fixing pass, see
`docs/architecture.md`.)

### AI service

99% line coverage (only a one-line settings factory uncovered). Phase 9 added one missing case:
mapping a real `AnalysisProviderError` to `502` through the actual endpoint (previously only
tested at the provider-unit level, not through the route).

### Frontend

The largest gap: **13.88%** statement coverage. Before Phase 9, only two files had tests at all
(`client.test.ts`, `Badges.test.tsx`). Phase 9 added:

- `authStore.test.ts` — session set/clear, subscriber notification, and localStorage
  rehydration on module reload.
- `ProtectedRoute.test.tsx` — the three routing outcomes (unauthenticated → `/login`, wrong role
  → `/`, allowed → renders).
- `useCategories.test.tsx` — one hook, as a pattern for testing the rest: a TanStack Query hook
  against an MSW-mocked backend, success and error states, plus a mutation.

**Still untested, and the natural next increment**: all 7 pages (`AdminPage`,
`CreateIncidentPage`, `DashboardPage`, `IncidentDetailPage`, `IncidentListPage`, `LoginPage`,
`RegisterPage`), `Layout.tsx`, `States.tsx`, `AuthContext.tsx` itself (only exercised indirectly
through `ProtectedRoute.test.tsx`'s mock), and the other three hooks (`useIncidents`, `useTeams`,
`useUsers`). This is real, known-remaining scope, not an oversight — Phase 9 prioritised the
security/routing-relevant pieces (auth state, route guarding) and backend RBAC/mapper gaps over
exhaustively covering every page in one pass.
