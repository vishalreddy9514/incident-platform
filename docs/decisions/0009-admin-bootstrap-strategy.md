# ADR-0009: Idempotent startup runner for the first ADMIN account, not a seed migration

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 6

## Context

Every ADMIN-only endpoint built in this phase (category management, team management, user role
changes, and previously user listing in Phase 5) needs at least one ADMIN account to exist to be
reachable at all. But `AuthService.register` always creates `Role.USER` accounts (a deliberate
Phase 5 decision — public registration should never hand out elevated privileges), and there is no
seeded admin account in the Flyway migrations. Some mechanism has to create the first admin.

## Options considered

1. **Seed an admin user in a Flyway migration**, similar to how V11 seeds incident categories.
   - Rejected outright, and for the same reason migration V11's own comment gives for *not*
     seeding demo users there: a real user account (even one meant to be immediately changed) is
     fundamentally different from reference/config data like categories. A hardcoded
     email/password-hash baked into a migration is either a real security liability if the
     password is ever left unchanged, or dead weight that has to be deleted in every real
     deployment — neither is acceptable for something version-controlled and applied automatically
     on every environment including production.
2. **A manual step**: document "run this SQL / call this script by hand after first deploy."
   - Pros: no application code needed.
   - Cons: easy to forget, inconsistent across environments (local dev, CI, staging, prod all need
     someone to remember), and not automatable — directly against the project's CI/CD and
     "runnable" goals.
3. **An idempotent `ApplicationRunner`** that creates exactly one ADMIN account on startup, only
   if none exists yet, using credentials from environment variables.
   - Pros: fully automatic and consistent across every environment; safe to leave running forever
     because it's a no-op once an admin exists (`countByRole(ADMIN) > 0` short-circuits it); doesn't
     touch the schema or migrations at all — it's ordinary application startup logic, not
     data-that-looks-like-schema; explicitly opt-in per environment (blank env vars = does
     nothing), so a shared/production environment where the first admin is provisioned some other
     way isn't forced to use this mechanism.
   - Cons: one more thing running on every startup (negligible — a single `COUNT` query).

## Decision

`AdminBootstrapRunner` (an `ApplicationRunner`) creates one ADMIN account on startup if and only
if `app.admin-bootstrap.email`/`.password` are both configured and no ADMIN account exists yet.
Local development sets these via `.env` (`ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD`);
leaving them blank (the reasonable default for a shared or production environment) means the
runner does nothing.

## Consequences

- `docker compose up` through to a working ADMIN login is fully automatic in local development —
  no manual SQL, no undocumented setup step.
- Once at least one ADMIN exists, further role changes go through the real, audited
  `PATCH /api/v1/users/{id}/role` endpoint (FR-19) — this runner is purely a bootstrapping
  mechanism for the very first admin, not an ongoing user-management tool.
- If the configured bootstrap email happens to already be registered as a non-admin account, the
  runner logs a warning and does nothing rather than silently overwriting or promoting an existing
  account — promotion still has to go through the audited endpoint once another admin exists.
