# ADR-0010: Refresh token in localStorage, access token in memory only

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 7

## Context

The frontend needs to keep the user logged in across a page reload without forcing re-entry of
credentials, while still working with the stateless JWT access token + opaque Redis-backed refresh
token design from ADR-0008. Where (and how) the frontend stores these two tokens is a real
security/UX tradeoff, not a default to reach for without thinking.

## Options considered

1. **Both tokens in memory only** (a JS variable, lost on reload).
   - Pros: nothing touches browser storage at all — no XSS-exfiltration surface for tokens.
   - Cons: every page reload forces a full re-login, which is a genuinely bad experience for an
     internal tool people keep open and refresh throughout a workday.
2. **Both tokens in `localStorage`.**
   - Pros: simplest to implement; survives reload.
   - Cons: a successful XSS attack can read `localStorage` directly and steal a *long-lived*
     refresh token, not just a token that expires in minutes — the worse version of the risk.
3. **Refresh token in an httpOnly cookie, set by the backend; access token in memory.**
   - Pros: the strongest option — an httpOnly cookie is invisible to JavaScript entirely, so XSS
     can't read the refresh token even in a worst-case compromise.
   - Cons: requires the backend to set/clear cookies (a `Set-Cookie` response header on
     login/refresh/logout) and switch the refresh flow from "send token in JSON body" to "rely on
     the cookie being sent automatically" — a real backend contract change, plus CSRF
     considerations for the cookie-based refresh endpoint specifically. A legitimate design for a
     production system, but more infrastructure than this project's current phase needs to take on
     alongside everything else Phase 7 is building.
4. **Refresh token in `localStorage`; access token in memory only** (the option taken).
   - Pros: survives reload (no forced re-login); the *short-lived* access token — the one actually
     sent on every request — never touches persistent storage at all, so even a successful XSS
     read of `localStorage` only yields a token an attacker still has to exchange (and which
     rotates out from under them the moment the legitimate user's next refresh fires); no backend
     contract change needed.
   - Cons: the refresh token itself is still readable by an XSS-compromised page — this is a real,
     accepted risk, not an eliminated one.

## Decision

Store the refresh token in `localStorage` (`authStore.ts`) and keep the access token in memory
only, re-issued transparently via `apiFetch`'s automatic refresh-and-retry on a 401. This is
Option 4 — a deliberate middle ground, not the strongest possible option.

## Consequences

- A page reload doesn't force re-login — the cached user renders immediately from
  `localStorage`, and the first API call transparently exchanges the refresh token for a new
  access token in the background.
- The project's XSS exposure is real but bounded: an attacker with script execution on the page
  can already do plenty of damage regardless of where tokens live (they can call the API directly
  as the logged-in user without ever touching a token) — this decision doesn't meaningfully change
  that baseline, it specifically limits what persists if the token is exfiltrated and the tab is
  later closed.
- If this project's threat model becomes stricter (e.g. genuinely public-facing rather than an
  internal tool), Option 3 (httpOnly cookie refresh token) is the documented upgrade path — it
  would touch `AuthController`'s refresh/login/logout endpoints and this file, not the JWT/RBAC
  design underneath.
