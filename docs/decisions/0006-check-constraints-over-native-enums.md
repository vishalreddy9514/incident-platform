# ADR-0006: Use VARCHAR + CHECK constraints for constrained columns, not native Postgres ENUM types

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 3

## Context

Several columns have a small, fixed set of valid values: `users.role`
(USER/ENGINEER/ADMIN), `incidents.status` (OPEN/IN_PROGRESS/ESCALATED/
RESOLVED/CLOSED), `incidents.priority` and `.severity`
(LOW/MEDIUM/HIGH/CRITICAL). PostgreSQL offers a native `CREATE TYPE ... AS
ENUM` for exactly this, as an alternative to a plain string column with a
`CHECK` constraint.

## Options considered

1. **Native Postgres ENUM type** (`CREATE TYPE incident_status AS ENUM (...)`).
   - Pros: smaller on-disk representation; the type itself documents valid
     values.
   - Cons: adding a new value requires `ALTER TYPE ... ADD VALUE`, which
     historically could not run inside the same transaction as other DDL in
     older Postgres versions (relevant since Flyway runs each migration in
     a transaction by default); removing or renaming a value is awkward
     (no direct `DROP VALUE`); the Java/JPA mapping is slightly more
     involved than a plain string.
2. **`VARCHAR` column + `CHECK` constraint** (the approach taken).
   - Pros: adding/changing valid values is a straightforward migration
     (`ALTER TABLE ... DROP CONSTRAINT ...; ALTER TABLE ... ADD CONSTRAINT
     ...`) with no transactional caveats; trivial JPA/Java mapping (a
     `String` or a Java `enum` mapped with `@Enumerated(EnumType.STRING)`);
     easier to explain and reason about in review or an interview.
   - Cons: slightly larger storage per row (negligible at this project's
     scale); the constraint isn't a reusable named type shared across
     columns (not needed here — no two columns share the same constrained
     value set).

## Decision

Use `VARCHAR` columns with explicit `CHECK` constraints (`chk_users_role`,
`chk_incidents_status`, `chk_incidents_priority`, `chk_incidents_severity`,
`chk_ai_analysis_predicted_priority`) for every constrained-value column in
the schema, rather than native Postgres ENUM types.

## Consequences

- Evolving the valid value set later (e.g. adding a `PENDING_CUSTOMER`
  incident status) is a simple, low-risk migration.
- The mapping to Java in Phase 4 stays simple: a Java `enum` with
  `@Enumerated(EnumType.STRING)`, validated on the way in by both the
  database constraint and Bean Validation — defence in depth rather than
  relying on either layer alone.
- This is consistent with ADR-0003 (role as a plain enum-like column, not a
  normalised table) — both decisions favour the simplest structure that
  correctly models a small, fixed, infrequently-changing value set, in
  line with the project's explicit "do not over-engineer" requirement.
