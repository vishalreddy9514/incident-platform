# ADR-0008: JWT access tokens + opaque Redis-backed refresh tokens, with manual credential verification

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 5

## Context

Phase 1 (§10) specified JWT authentication with short-lived access tokens and longer-lived,
rotated refresh tokens. Two implementation questions needed resolving: what a refresh token
actually *is* (another JWT, or something else), and whether login should go through Spring
Security's `AuthenticationManager`/`AuthenticationProvider` machinery or be handled directly.

## Decision 1: Refresh tokens are opaque, Redis-backed, and rotating — not JWTs

### Options considered

1. **A second JWT as the refresh token**, longer-lived than the access token.
   - Pros: no extra infrastructure — same signing/verification code as the access token.
   - Cons: stateless by nature, so it **can't be revoked** before it expires — there's no way to
     invalidate a refresh token on logout or if a device is compromised, short of maintaining a
     blocklist (which reintroduces server-side state anyway, just in a worse shape).
2. **An opaque random token, stored server-side in Redis** (token → user id, with a TTL).
   - Pros: genuinely revocable (`DEL` the key), naturally supports rotation (delete-and-reissue on
     each use, so a stolen-but-already-used refresh token stops working), and Redis is already in
     the stack for caching/rate limiting — no new infrastructure.
   - Cons: a Redis round-trip on every refresh (negligible latency at this scale); refresh tokens
     don't carry their own claims, so a DB/Redis lookup is required to resolve them (acceptable —
     refreshing is infrequent compared to normal request volume, unlike access-token validation).

### Decision

Refresh tokens are opaque UUIDs stored in Redis as `refresh-token:<token> → userId`, with a TTL
matching `app.jwt.refresh-token-ttl-days`. Each use deletes the token as part of validating it
(`RefreshTokenService.validateAndRevoke`) and issues a new one — satisfying the "rotated"
requirement from Phase 1 structurally, not just by convention.

## Decision 2: Manual credential verification, not Spring Security's `AuthenticationManager`

### Options considered

1. **Wire a `DaoAuthenticationProvider` + `AuthenticationManager`**, the idiomatic Spring Security
   pattern for username/password login.
   - Pros: idiomatic; integrates with Spring Security's event/audit hooks if the project grows to
     need them.
   - Cons: that machinery exists to support *pluggable, potentially multiple* authentication
     sources (LDAP, multiple providers, etc.) — genuine complexity this project doesn't need for a
     single-source (`users` table), stateless JWT API. Wiring it adds several extra beans and a
     layer of indirection over what is, underneath, still just "look up the user, compare the
     password."
2. **Verify credentials directly in `AuthService.login`**: repository lookup + `PasswordEncoder
   .matches`.
   - Pros: the entire login flow is readable top-to-bottom in one method; no framework machinery
     to configure correctly; still uses `PasswordEncoder` (BCrypt) for the actual security-critical
     comparison, so no security property is lost — only the *pluggable provider* abstraction is
     skipped.
   - Cons: doesn't benefit from Spring Security's built-in authentication event publishing; judged
     an acceptable tradeoff at this project's scale.

### Decision

`AuthService.login` performs the lookup and `PasswordEncoder.matches` comparison directly. No
`AuthenticationManager` bean exists in this codebase. Authenticating *subsequent* requests (via
the issued JWT) is handled separately by `JwtAuthenticationFilter`, which populates the
`SecurityContext` directly from a validated token — also without going through
`AuthenticationManager`.

## Consequences

- Logout and token compromise are handled correctly: revoking a refresh token is a real, immediate
  operation, not something that has to wait out a token's natural expiry.
- The login code path is simple enough to fully read and reason about in one method — a genuine
  benefit for a project meant to be explained in an interview.
- If this project later needs multiple authentication sources (SSO, OAuth2 login, etc.), that's a
  legitimate trigger to revisit this decision and adopt the standard `AuthenticationManager`
  pattern then — not something to build speculatively now.
