# API Documentation

> **Status:** growing incrementally as endpoints are built. The
> authoritative contract is the generated OpenAPI spec (springdoc, served
> at `/v3/api-docs` and browsable at `/swagger-ui.html` once the backend
> is running) so it can never drift from the code. This document adds
> narrative context around it.

## Implemented so far (Phase 4)

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/v1/categories` | none yet (Phase 4 scaffolding — see `SecurityConfig`) | Lists active incident categories, ordered by name |
| `GET` | `/actuator/health` | none | Spring Boot Actuator health check |

Error responses follow the standard envelope (Phase 1 §9.1):

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

Full endpoint list as planned: see
[`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md) §9.
