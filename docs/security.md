# Security

> **Status:** authentication/authorisation implemented (Phase 5). Full
> write-up (threat model summary, dependency/image scanning results,
> secrets handling in production) still comes in Phase 15 — this section
> documents what's actually built so far, not the eventual complete
> picture.
>
> Full security architecture as originally planned: see
> [`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md) §10.

## Authentication (Phase 5)

- **Password storage**: BCrypt (`BCryptPasswordEncoder`), via Spring
  Security's `PasswordEncoder` abstraction. Never logged, never returned
  in any DTO — `User.getPasswordHash()` is only ever read by `AuthService`
  and `CustomUserDetails`.
- **Access tokens**: JWT, HS256, 15-minute default TTL. Signing key comes
  from `app.jwt.secret` (env-configured, no default weak enough to slip
  into production unnoticed — the default itself is 61 bytes, well over
  the 32-byte HS256 minimum, so a misconfigured short secret fails loudly
  at startup rather than silently signing with a weak key).
- **Refresh tokens**: opaque random strings, stored server-side in Redis
  with a TTL, rotated (deleted-and-reissued) on every use — see
  ADR-0008. This makes them genuinely revocable, unlike a second JWT.
- **Login**: direct credential verification in `AuthService`, not via
  Spring Security's `AuthenticationManager` — see ADR-0008.

## Authorisation (RBAC)

- `@EnableMethodSecurity` + `@PreAuthorize` for role-gated endpoints
  (e.g. `GET /api/v1/users` is ADMIN-only), following the explicit
  per-action pattern from ADR-0002 rather than a role hierarchy.
- `JwtAuthenticationFilter` re-loads the user from the database on every
  request (via `CustomUserDetailsService`), so a deactivated account
  (`is_active = false`) stops working immediately — not just after its
  current access token happens to expire.

## Request-level protections

- **Rate limiting**: Redis-backed fixed window, applied to
  `/api/v1/auth/login` and `/api/v1/auth/register` (5 requests per 60
  seconds by default, configurable via `app.rate-limit.auth.*`) — the
  endpoints most exposed to credential-guessing/enumeration.
- **CORS**: explicit allow-list (`app.cors.allowed-origin`, defaults to
  the local frontend dev server), not a wildcard.
- **Stateless sessions**: no server-side session state
  (`SessionCreationPolicy.STATELESS`) — every request is authenticated
  independently via its bearer token.
- **CSRF**: disabled. Standard and safe for a stateless, token-authenticated
  API with no cookie-based session to forge requests against.

## Error handling

`RestAuthenticationEntryPoint` (401) and `RestAccessDeniedHandler` (403)
return the same JSON error envelope as every other error path
(`GlobalExceptionHandler`), rather than Spring Security's default empty
or HTML responses — the API has one consistent error shape throughout.

Login deliberately returns the same `INVALID_CREDENTIALS` error for both
"no such email" and "wrong password", so failed login attempts can't be
used to enumerate registered email addresses.

## Audit logging

Register and login events are written to `AuditLog` (the append-only
table from Phase 3) via `AuditLogRepository`. Role changes and other
admin actions will extend this in Phase 6 as those endpoints are built.

## Not yet built (tracked for later phases)

- Global exception-handling coverage for `AuthenticationException` thrown
  outside the JWT filter (not currently needed — no other code path
  throws it) — noted here so it isn't forgotten if that changes.
- Dependency/container image vulnerability scanning (Phase 11/15).
- A documented threat model and full secrets-management story for AWS
  (Phase 15).
