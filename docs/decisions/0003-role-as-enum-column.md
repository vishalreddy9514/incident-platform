# ADR-0003: Model role as an enum column on User, not a normalised roles/permissions table

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 1

## Context

The platform has exactly three fixed roles (USER, ENGINEER, ADMIN), known in advance and not expected to change dynamically at runtime. The question is how to model this in the database: a simple enum column on `User`, or a fully normalised `Role` and `UserRole`/`Permission` table structure (the pattern used when roles are user-configurable or permissions need fine-grained composition).

## Options considered

1. **Enum column on `User`** (`role VARCHAR` constrained to `USER`/`ENGINEER`/`ADMIN`, or a native Postgres enum type).
   - Pros: simple, fast to query, matches the fixed and small role set; no extra joins for the most common query ("what can this user do").
   - Cons: adding a fourth role or making permissions independently composable later requires a migration, not just data changes.
2. **Normalised `Role` + `Permission` + join tables**, fully data-driven RBAC.
   - Pros: new roles/permissions could be added without code changes; standard pattern in larger systems.
   - Cons: meaningful upfront complexity (multiple tables, joins on every auth check) for a requirement the project doesn't actually have — three fixed roles, not a permissions marketplace. Directly conflicts with the "do not over-engineer" requirement from Phase 1.

## Decision

Model `role` as an enum column on the `User` entity (§8.1). If role/permission requirements grow in a way that genuinely needs data-driven configuration, this is a documented candidate for a future migration — not solved speculatively now.

## Consequences

- Auth checks (`@PreAuthorize("hasRole('ENGINEER')")`) stay simple and fast, with no extra joins.
- If a fourth role or truly dynamic permissions are ever needed, it's a real migration (schema change + data backfill), not a config change — an accepted, explicitly documented tradeoff rather than an oversight.
- This decision pairs with ADR-0002: role is a coarse gate (who *can* generally do this kind of thing), while fine-grained/ownership rules are handled in the service layer rather than by inventing more roles.
