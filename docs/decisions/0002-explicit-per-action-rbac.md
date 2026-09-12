# ADR-0002: Explicit per-action RBAC checks, not a purely hierarchical role level

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 1

## Context

The platform has three roles — USER, ENGINEER, ADMIN — and a set of permissions (create incident, assign incident, manage categories, view audit log, etc.). The simplest implementation would give each role a numeric level (USER=1, ENGINEER=2, ADMIN=3) and gate actions on "role level >= required level."

That model breaks down for this domain: some ADMIN-only actions (e.g. managing incident categories, viewing audit logs) have nothing to do with "more incident-handling seniority" than an ENGINEER — they're a different kind of permission, not a higher tier of the same one. Conversely, some USER actions (viewing/commenting on *their own* incident) aren't about role level at all — they're about ownership.

## Options considered

1. **Numeric role hierarchy** (`role.level >= required.level`).
   - Pros: trivial to implement, one comparison.
   - Cons: forces every permission onto a single ordering that doesn't actually hold for this domain (category management isn't "ENGINEER + 1"); ownership-based rules (a USER can only see their own incidents) can't be expressed this way at all.
2. **Explicit per-action permission checks**, using Spring Security's `@PreAuthorize` for role-based gates plus explicit service-layer checks for ownership-based rules.
   - Pros: each endpoint/action states its actual requirement rather than inheriting one from a hierarchy; ownership rules are expressed directly instead of bolted on; matches how real enterprise RBAC systems are usually built (permission-based, not just level-based).
   - Cons: more annotations/checks to write and keep consistent; requires discipline (and tests — see Phase 1 §11 "backend security" test layer) to avoid gaps.
3. **Full permission/role table in the database** (many-to-many roles↔permissions).
   - Considered and deliberately deferred — see ADR-0003. Would solve the same problem with more flexibility but more upfront complexity than this project's scope justifies at v1.

## Decision

Use Spring Security's `@PreAuthorize` for role-gated endpoints, combined with explicit service-layer ownership checks (e.g. "is the current user the creator of this incident, or an ENGINEER/ADMIN") where role alone isn't sufficient. No numeric role hierarchy.

## Consequences

- Every controller method's access rule is visible and testable in isolation — directly supports the "backend security" test layer in the testing strategy (Phase 1 §11).
- Adding a new permission means adding an explicit check, not just adjusting a number — more deliberate, less error-prone by omission, but requires each new endpoint to explicitly state its access rule rather than inheriting one for free.
- This is a good interview talking point: it demonstrates recognising that role hierarchies are a common but leaky abstraction for real-world permission models.
