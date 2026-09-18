# Security

> **Status:** Complete for this project's scope (Phase 15). Authentication/authorisation
> (Phase 5), CI-integrated scanning (Phase 11), infrastructure least-privilege (Phase 12), and the
> threat model / hardening pass below (Phase 15) are all built and verified, not just planned.
> Deliberately out of scope, with reasoning: TLS (ADR-0013 - no domain exists yet to issue a
> certificate against), a WAF, and MFA (Phase 1 §13 explicitly scoped both out for a portfolio
> project of this size).
>
> Full security architecture as originally planned: see
> [`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md) §10.

## Threat model

**Assets worth protecting**: user credentials/password hashes, JWT signing key and refresh tokens
(Redis), incident data (may describe internal systems/vulnerabilities), the admin-bootstrap
credential, the AI service's internal token, and (if configured) a real LLM provider API key.

**Trust boundaries**: browser ↔ ALB/backend (untrusted - anyone on the internet), backend ↔
Postgres/Redis (trusted - same VPC, security-group-restricted to the backend only), backend ↔ AI
service (semi-trusted - internal token required, but modelled as a real boundary rather than
implicitly trusted, since the AI service ultimately talks to an external LLM provider).

| # | Threat | Mitigation | Where |
|---|---|---|---|
| 1 | Credential stuffing / brute-force login | Redis-backed rate limiting on `/auth/login` and `/auth/register`; BCrypt (slow by design) | `RateLimitingFilter` |
| 2 | Email enumeration via login errors | Same `INVALID_CREDENTIALS` response for "no such user" and "wrong password" | `AuthService` |
| 3 | Stolen/leaked access token used after logout or role change | 15-minute TTL; user re-loaded from DB on every request, so a deactivated account or changed role takes effect immediately, not just at token expiry | `JwtAuthenticationFilter` |
| 4 | Stolen refresh token reused after rotation | Opaque, server-side (Redis), rotated (deleted-and-reissued) on every use - reuse of an old token is detectable and rejected | ADR-0008, `AuthenticationIntegrationTest.refreshTokenRotatesAndTheOldTokenCannotBeReused` |
| 5 | A USER escalating to see/modify another user's incidents, or acting as ENGINEER/ADMIN | Explicit per-action `@PreAuthorize` (ADR-0002) for role checks; separate service-layer ownership checks (`IncidentService`) for "a USER only sees their own incidents" - a role check alone can't express that rule | `IncidentService`, RBAC-denial tests (Phase 9) |
| 6 | Cross-site request forgery | Not applicable - stateless bearer-token auth, no cookie-based session to forge a request against; CSRF protection is correctly *disabled*, not missing | `SecurityConfig` |
| 7 | XSS via a stored incident title/description rendered in the frontend | React escapes rendered text by default (no `dangerouslySetInnerHTML` in this codebase); CSP `script-src 'self'` as defense-in-depth even if that were ever violated | `frontend/nginx.conf.template` |
| 8 | Clickjacking (the app framed on a malicious page) | `X-Frame-Options: DENY` and CSP `frame-ancestors 'none'` | `frontend/nginx.conf.template` |
| 9 | MIME-sniffing a response into an unintended content type | `X-Content-Type-Options: nosniff` | backend `SecurityConfig` (Spring Security default) + `frontend/nginx.conf.template` |
| 10 | Cross-origin data exfiltration via a malicious third-party site | Explicit CORS allow-list (`app.cors.allowed-origin`), not a wildcard | `SecurityConfig` |
| 11 | Referrer leakage exposing an internal URL/token in a query string to a third-party link | `Referrer-Policy: strict-origin-when-cross-origin` | backend + frontend |
| 12 | AI service reachable/callable directly, bypassing backend-enforced RBAC | Never exposed via the ALB (Terraform: no target group registers it) or the frontend (ADR-0001); requires the shared `X-Internal-Token` even so, for defence-in-depth | `infrastructure/terraform/modules/ecs`, `app/security.py` |
| 13 | The AI service used as a pathway to read/write Postgres | Its IAM task role and application code have no database access or credentials at all - architecturally impossible, not just unused | `docs/architecture.md`, `modules/iam` |
| 14 | A vulnerable dependency (known CVE) shipped in an image | OWASP Dependency-Check (backend), `pip-audit` (ai-service), `npm audit` (frontend), Trivy (all three images, blocking on HIGH/CRITICAL) | `.github/workflows/pr.yml`, `deploy.yml` |
| 15 | A secret accidentally committed to the repository | gitleaks on every PR (`.gitleaks.toml`); Secrets Manager (Terraform) / `.env` (git-ignored) are the only places real secrets live | `.github/workflows/security.yml` |
| 16 | A code-level vulnerability (e.g. injection, unsafe deserialisation) | CodeQL static analysis across all three languages on every PR | `.github/workflows/security.yml` |
| 17 | A Terraform misconfiguration (e.g. a public S3 bucket, an over-broad security group) | Trivy config scan against `infrastructure/terraform`, blocking on HIGH/CRITICAL | `.github/workflows/security.yml` |
| 18 | Traffic sniffed in transit | **Accepted risk for now** - HTTP only until a real domain exists for TLS (ADR-0013); not applicable to `localhost` dev traffic | ADR-0013 |

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
- **Ownership checks beyond role checks**: `@PreAuthorize` alone can't express "a USER may only
  view/modify their own incidents" - `IncidentService` applies that rule explicitly in the
  service layer, on every read and write path, not just the common ones (Phase 1 §10's design,
  not a retrofit).

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

## Security headers (Phase 15)

Both the backend (`SecurityConfig`) and the frontend (`nginx.conf.template`) send:

- `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` — Spring Security's own defaults on
  the backend; explicit on the frontend, since nginx has no equivalent built in.
- `Referrer-Policy: strict-origin-when-cross-origin` — explicit on both (not a Spring Security
  default).
- `Permissions-Policy` — explicit on both, denying browser features (camera/microphone/
  geolocation/payment/USB) neither service has any use for.
- **Frontend only, `Content-Security-Policy`**: `default-src 'self'`, `script-src 'self'` (no
  `unsafe-inline`/`unsafe-eval` - nothing in this app needs either), `style-src 'self'
  'unsafe-inline'` (several components use React's `style={{...}}` prop, which CSP treats as
  inline styling), `connect-src 'self'` plus the backend's origin (templated in at container
  start via `API_ORIGIN` - see the comment in `nginx.conf.template` for why that's
  environment-dependent), `frame-ancestors 'none'`, `object-src 'none'`.
- The backend has no `Content-Security-Policy` of its own - it serves JSON, not HTML, so CSP
  (a browser rendering-context control) doesn't apply to it.

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
table from Phase 3) via `AuditLogRepository`. Role changes are also
audited (Phase 6, `USER_ROLE_CHANGED`).

## Frontend token storage (Phase 7)

The refresh token is stored in `localStorage`; the access token is kept
in memory only, never persisted. This is a deliberate middle-ground
tradeoff, not the strongest possible option — see ADR-0010 for the full
reasoning and the documented upgrade path (an httpOnly-cookie refresh
token) if this project's threat model ever calls for it.

## Dependency, image, and infrastructure scanning

- **Dependency vulnerabilities** (Phase 11): OWASP Dependency-Check (backend), `pip-audit`
  (ai-service), `npm audit` (frontend) - all non-blocking (`continue-on-error: true`), since a
  transitive low/moderate finding shouldn't block every PR, but real findings are visible in each
  step's own log.
- **Container images** (Phase 11): Trivy scans all three built images in `deploy.yml`, blocking
  on HIGH/CRITICAL before anything is pushed.
- **Infrastructure-as-code** (Phase 15): Trivy also scans `infrastructure/terraform` itself for
  misconfigurations (`.github/workflows/security.yml`), blocking on HIGH/CRITICAL.
- **Static analysis / SAST** (Phase 15): CodeQL, matrixed across Java, Python, and
  TypeScript, on every PR - the free, GitHub-native alternative to the SonarCloud integration
  `docs/testing.md` documents as a gap (SonarCloud needs an external project + token that don't
  exist; CodeQL needs neither).
- **Secret scanning** (Phase 15): gitleaks (`.gitleaks.toml` allowlists this project's own
  documented `change-me-*` placeholder convention and test-fixture files, so real findings aren't
  drowned out).
- **Proactive dependency updates** (Phase 15): Dependabot (`.github/dependabot.yml`) opens a PR
  when a newer version exists for Maven, pip, npm, each Dockerfile's base image, Terraform
  providers, and GitHub Actions themselves - the complement to the reactive scans above.

## Secrets management

- **Local development**: `.env` (git-ignored; `.env.example` is the committed, placeholder-only
  template - see `.gitleaks.toml`'s allowlist for that exact convention). Never baked into a
  Docker image; injected at container start via `env_file`/`environment` in `docker-compose.yml`.
- **AWS** (Phase 12): AWS Secrets Manager (`infrastructure/terraform/modules/secrets`) - DB
  credentials, JWT signing key, admin-bootstrap credential, the AI-service internal token, and
  the LLM API key are all real Secrets Manager entries, injected into ECS task definitions via
  the `secrets` field (resolved by the execution role at task-start, never present in the task
  definition or image itself - `modules/ecs/main.tf`). No secret value ever has a real default in
  Terraform; each requires an explicit `terraform.tfvars` (git-ignored) or `TF_VAR_*` value.
- **CI/CD**: `pr.yml` structurally never has deploy credentials (`permissions: contents: read`,
  ADR-0005); `deploy.yml`'s GHCR push uses only the workflow's own short-lived `GITHUB_TOKEN`.
  The GitHub Actions OIDC deploy role (ADR-0014, Phase 13) means the eventual ECR cutover won't
  need a long-lived AWS key as a GitHub secret either.

## Not yet built (out of scope, with reasoning - not forgotten)

- **TLS** — no real domain exists yet to issue a certificate against (ADR-0013); deferred to
  Phase 13's eventual real cloud deployment, not skipped silently.
- **WAF, MFA** — explicitly out of scope per Phase 1 §13 for a project at this scale; the cost/
  complexity isn't justified to demonstrate the skill here.
- **Alertmanager routing for the security-relevant Prometheus alerts** (Phase 14) — the alerts
  exist and are evaluated; routing them to a real destination (Slack/email/PagerDuty) needs one
  to exist, which it doesn't for this portfolio project (same documented gap as Phase 12's
  CloudWatch SNS topic).
- Global exception-handling coverage for `AuthenticationException` thrown
  outside the JWT filter (not currently needed — no other code path
  throws it) — noted here so it isn't forgotten if that changes.
