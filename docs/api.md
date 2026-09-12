# API Documentation

> **Status:** growing incrementally as endpoints are built. The
> authoritative contract is the generated OpenAPI spec (springdoc, served
> at `/v3/api-docs` and browsable at `/swagger-ui.html` once the backend
> is running) so it can never drift from the code. This document adds
> narrative context around it.

## Implemented so far (Phase 6)

### Auth (public, rate-limited on login/register)

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Creates a USER account, returns tokens |
| `POST` | `/api/v1/auth/login` | Verifies credentials, returns fresh tokens |
| `POST` | `/api/v1/auth/refresh` | Rotates a valid refresh token for a new pair |
| `POST` | `/api/v1/auth/logout` | Revokes the given refresh token |

### Users (authenticated)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | any user | Own profile |
| `GET` | `/api/v1/users` | ADMIN | Paginated user list |
| `PATCH` | `/api/v1/users/{id}/role` | ADMIN | Change a user's role (FR-19); rejects self-edit |

### Categories (authenticated)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/v1/categories` | any user | Active categories, ordered by name |
| `POST` | `/api/v1/categories` | ADMIN | Create a category |
| `PUT` | `/api/v1/categories/{id}` | ADMIN | Update name/description/active flag |

### Teams (authenticated)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/v1/teams` | any user | List all teams |
| `POST` | `/api/v1/teams` | ADMIN | Create a team |

### Incidents (authenticated) — the core domain

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/v1/incidents` | any user | Create (FR-5) |
| `GET` | `/api/v1/incidents` | any user | List/search/filter (FR-6); USER results auto-scoped to their own |
| `GET` | `/api/v1/incidents/{id}` | owner or ENGINEER/ADMIN | Detail |
| `PUT` | `/api/v1/incidents/{id}` | owner (limited) or ENGINEER/ADMIN | Update fields; status/priority/severity changes need ENGINEER/ADMIN |
| `DELETE` | `/api/v1/incidents/{id}` | ADMIN | Delete (cascades to comments/history/assignments) |
| `POST` | `/api/v1/incidents/{id}/comments` | owner or ENGINEER/ADMIN | Add a comment (FR-10) |
| `GET` | `/api/v1/incidents/{id}/comments` | owner or ENGINEER/ADMIN | List comments |
| `GET` | `/api/v1/incidents/{id}/history` | owner or ENGINEER/ADMIN | Timeline (FR-11) |
| `POST` | `/api/v1/incidents/{id}/assign` | ENGINEER/ADMIN | Assign to an engineer/admin (FR-8); auto-moves OPEN → IN_PROGRESS |
| `POST` | `/api/v1/incidents/{id}/escalate` | ENGINEER/ADMIN | Escalate, with an optional reason |

### Dashboard (authenticated)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/v1/dashboard/metrics` | Role-scoped incident counts by status (FR-16/17/18) |

### Other

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/actuator/health` | none | Health check |

## Status transition rules

```
OPEN        → IN_PROGRESS, ESCALATED, CLOSED
IN_PROGRESS → ESCALATED, RESOLVED, OPEN
ESCALATED   → IN_PROGRESS, RESOLVED
RESOLVED    → CLOSED, IN_PROGRESS
CLOSED      → (terminal — no further transitions)
```

An out-of-list transition returns `409 CONFLICT` with error code
`INVALID_STATUS_TRANSITION`.

## Error responses

Follow the standard envelope (Phase 1 §9.1):

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Incident not found: 42",
    "details": null
  }
}
```

Error codes introduced so far: `EMAIL_ALREADY_REGISTERED` (409),
`INVALID_CREDENTIALS` (401), `ACCOUNT_DISABLED` (403),
`INVALID_REFRESH_TOKEN` (401), `RATE_LIMITED` (429), `UNAUTHORIZED`
(401), `FORBIDDEN` (403), `NOT_FOUND` (404), `VALIDATION_FAILED` (400),
`INVALID_STATUS_TRANSITION` (409), `INVALID_ASSIGNEE` (400),
`CATEGORY_NAME_TAKEN` (409), `TEAM_NAME_TAKEN` (409),
`CANNOT_CHANGE_OWN_ROLE` (400).

Full endpoint list as originally planned: see
[`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md) §9.
