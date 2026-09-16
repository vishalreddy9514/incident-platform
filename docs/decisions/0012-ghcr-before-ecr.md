# ADR-0012: Push images to GHCR now, migrate to ECR once Terraform/AWS exist

**Status:** Accepted
**Date:** 2026-09-16
**Phase:** 11

## Context

ADR-0005 and Phase 1 §12 both describe `deploy.yml` pushing built images to Amazon ECR as step 4
of the main-branch pipeline. But Phase 11 (this phase, CI/CD) runs before Phase 12 (Terraform) and
Phase 13 (cloud deployment) — there is no AWS account wired into this repository yet, no ECR
repository, and no `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` (or OIDC role) secrets configured.

## Options considered

1. **Build `deploy.yml` against ECR now anyway**, with the AWS steps present but effectively
   dead until Phase 12/13 configure real credentials.
   - Pros: matches the originally-planned pipeline exactly; no rewrite needed later.
   - Cons: every merge to `main` would either fail outright (missing secrets) or need the AWS
     steps wrapped in an `if:` that silently no-ops — both are exactly the kind of
     placeholder/fake functionality this project's Definition of Done rules out. A red or
     silently-skipped required check on every merge is worse than not having the check yet.
2. **Push to GHCR (GitHub Container Registry) now; migrate to ECR when Phase 12/13 land.**
   - Pros: `docker/login-action` against GHCR needs only the built-in `GITHUB_TOKEN` — no
     external account setup, no secrets to configure, genuinely green today. Still exercises the
     real parts of the pipeline (build, Trivy scan blocking on HIGH/CRITICAL, push, tag by SHA)
     that don't depend on which registry is the destination.
   - Cons: a real migration step later (swap the login/push target, likely an OIDC role rather
     than long-lived keys) — accepted as a natural consequence of building CI/CD before the cloud
     infrastructure it will eventually deploy to, not extra work invented for its own sake.
3. **Skip the push step entirely until Phase 13.**
   - Rejected: building and Trivy-scanning images without ever completing the push leaves the
     "build, scan, and push images" step from Phase 1 §12 only two-thirds real, and the push step
     itself (auth, tagging, registry interaction) is exactly the part worth proving works now
     rather than debugging for the first time alongside Terraform in Phase 12/13.

## Decision

`deploy.yml` builds all three images, scans them with Trivy (blocking on HIGH/CRITICAL, matching
Phase 1 §12 step 3 exactly), and pushes them to `ghcr.io/<owner>/<repo>-<service>:<sha>` and
`:latest` using the workflow's own `GITHUB_TOKEN`. The ECR push, `terraform plan`/`apply`, ECS
task definition update, and smoke test steps are not yet built — they're deferred to Phase 12
(Terraform) and Phase 13 (cloud deployment), not faked with placeholder steps.

## Consequences

- `deploy.yml` is fully real and green today, with no secrets to configure and nothing that
  silently no-ops.
- The security boundary ADR-0005 cares about (PR runs never get deploy credentials) holds
  regardless of which registry is the target — `pr.yml` still never has `packages: write`.
- Phase 12/13 has a concrete migration to do: swap GHCR for ECR (likely via OIDC federation
  rather than long-lived AWS keys, itself worth a future ADR), then add the
  terraform/ECS/smoke-test steps this ADR explicitly deferred.
- Images published to GHCR during Phase 11-12 are portfolio/demo artifacts, not a production
  registry commitment — nothing about this decision implies GHCR is the long-term choice.
