# ADR-0014: GitHub Actions authenticates to AWS via OIDC, not long-lived keys

**Status:** Accepted
**Date:** 2026-09-16
**Phase:** 13

## Context

`deploy.yml` needs to authenticate to AWS eventually (to push images to the ECR repositories
Phase 12 created, per ADR-0012's migration note). The two realistic options for a GitHub Actions
workflow are a long-lived IAM user access key stored as a repository secret, or short-lived
credentials obtained per-run via OpenID Connect (OIDC) federation.

## Options considered

1. **IAM user access key as a `secrets.AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` pair.** Simple
   to set up, but the credential is long-lived, has to be manually rotated, and - if the secret
   ever leaked (a compromised Action, a misconfigured log, a fork PR) - would grant standing
   access to whatever the IAM user can do until someone notices and revokes it.
2. **OIDC federation** (`aws_iam_openid_connect_provider` + a role with a trust policy scoped to
   `repo:vishalreddy9514/incident-platform:ref:refs/heads/main`). No credential exists anywhere
   outside a single workflow run - GitHub mints a token, AWS verifies it against the trust policy
   and issues short-lived STS credentials for that run only. Nothing to rotate, nothing that
   outlives the job, and the trust policy itself is the access boundary (only `deploy.yml` running
   on `main` in this exact repo can ever assume the role - not a PR run, not a fork, not any other
   workflow).

## Decision

`modules/iam/github_oidc.tf` provisions the OIDC provider and a deploy role scoped, for now, to
exactly what `deploy.yml` needs today: `ecr:GetAuthorizationToken` plus push permissions on this
project's own ECR repositories - not a broad "let CI run `terraform apply`" role, which would be a
materially bigger trust decision than "let CI push images" and isn't needed yet.

## Consequences

- Zero cost to have this exist (IAM resources aren't billed) and zero credentials to manage -
  genuinely free prep work, unlike everything else in Phase 13, which requires spending real money
  to actually run.
- `deploy.yml` isn't wired to use this role yet: doing so only makes sense once the role has
  actually been created in a real AWS account (this module is written and `terraform validate`-
  clean, not applied - see `docs/deployment.md`'s Phase 13 section for why). Wiring it in now would
  reference a role ARN that doesn't exist, breaking the workflow for no benefit.
- `create_github_oidc_provider` defaults to `true` but can be set `false` if the target AWS account
  already has a `token.actions.githubusercontent.com` OIDC provider from another project - only one
  may exist per account regardless of how many roles trust it.
- A future, separate decision if this project ever wants CI to run `terraform apply` itself: that
  role would need much broader permissions (VPC, RDS, ECS, IAM, etc.) and deserves its own trust
  review, not an expansion of this narrowly-scoped deploy role.
