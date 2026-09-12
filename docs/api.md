# API Documentation

> **Status:** growing incrementally as endpoints are built. The
> authoritative contract is the generated OpenAPI spec (springdoc, served
> at `/v3/api-docs` and browsable at `/swagger-ui.html` once the backend
> is running) so it can never drift from the code. This document adds
> narrative context around it.

## Implemented so far (Phase 5)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | none (rate-limited) | Creates a USER account, returns `accessToken` + `refreshToken` |
| `POST` | `/api/v1/auth/login` | none (rate-limited) | Verifies credentials, returns fresh tokens |
| `POST` | `/api/v1/auth/refresh` | none | Exchanges a valid refresh token for a new token pair (old refresh token is revoked — rotation) |
| `POST` | `/api/v1/auth/logout` | none | Revokes the given refresh token |
| `GET` | `/api/v1/categories` | **required** | Lists active incident categories, ordered by name |
| `GET` | `/api/v1/users/me` | **required** | Returns the current authenticated user's profile |
| `GET` | `/api/v1/users` | **required, ADMIN only** | Paginated list of all users |
| `GET` | `/actuator/health` | none | Spring Boot Actuator health check |

Authenticated requests use `Authorization: Bearer <accessToken>`. Access
tokens are short-lived (15 min default); use `/api/v1/auth/refresh` to
get a new pair without re-entering credentials.

### Auth response shape

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "3fa2c1e0-...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "user": {
    "id": 1,
    "email": "you@example.com",
    "displayName": "Your Name",
    "role": "USER",
    "teamId": null,
    "isActive": true
  }
}
```

### Error responses

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

Validation failures (`400`) populate `details` with a field→message map.
Auth-specific error codes: `EMAIL_ALREADY_REGISTERED` (409),
`INVALID_CREDENTIALS` (401, used for both unknown email and wrong
password — deliberately generic), `ACCOUNT_DISABLED` (403),
`INVALID_REFRESH_TOKEN` (401), `RATE_LIMITED` (429), `UNAUTHORIZED` (401,
missing/invalid access token), `FORBIDDEN` (403, valid token but
insufficient role).

Full endpoint list as planned: see
[`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md) §9.
