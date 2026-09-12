# ADR-0005: Split CI/CD into two pipelines (PR verification vs main deployment)

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 1

## Context

The project needs automated checks on proposed changes (linting, tests, security scanning) and a separate process for actually shipping accepted changes (building images, provisioning infra, deploying, smoke-testing). These could be combined into a single workflow triggered on every push, or explicitly split by trust level and trigger.

## Options considered

1. **Single combined workflow** on every push to any branch — run tests, and if on `main`, also deploy.
   - Pros: one file to maintain.
   - Cons: blurs two different concerns with different trust requirements — verifying *proposed* code shouldn't have any ability to touch real infrastructure or deploy, but a single workflow with conditional deploy steps makes that boundary easy to get wrong (e.g. a misconfigured `if:` condition accidentally deploying from a PR branch).
2. **Two separate workflows**: `pr.yml` (runs on pull requests — lint, unit tests, integration tests, security/dependency scanning, E2E against a local Compose stack) and `deploy.yml` (runs only on merge to `main` — rebuild, image scan, push to ECR, `terraform plan`/`apply` behind manual approval, deploy, smoke test).
   - Pros: the trust boundary is structural (different trigger, different file, different required secrets/permissions) rather than a conditional inside one file; `pr.yml` never has AWS credentials available to it at all, which is a real security property, not just a convention.
   - Cons: some duplication between the two workflows (both build/test the code) — accepted as a reasonable cost for the clearer boundary.

## Decision

Maintain two GitHub Actions workflows: `.github/workflows/pr.yml` for verification on every pull request, and `.github/workflows/deploy.yml` for build-and-deploy on merge to `main`, with `terraform apply` gated behind manual approval rather than fully automatic.

## Consequences

- PR runs never have access to AWS credentials or deployment secrets — those are only exposed to the `deploy.yml` workflow, scoped to the `main` branch.
- The manual approval gate on `terraform apply` reflects realistic practice (infra changes aren't blindly auto-applied) and is itself a talking point about safe deployment practice, not just a limitation of the pipeline.
- Slightly more CI config to maintain than a single workflow, judged worth it for the clearer security boundary and for demonstrating an understanding of *why* PR verification and deployment are different concerns — directly useful in an interview context.
