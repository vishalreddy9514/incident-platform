# Security Policy

This is a portfolio project, not a production service handling real user data - but it's built
and tested the way a real one would be, and that includes how it handles a reported issue.

## Reporting a vulnerability

Please open a [GitHub issue](https://github.com/vishalreddy9514/incident-platform/issues) or
contact the repository owner directly rather than disclosing a finding publicly first. There's no
formal SLA (this isn't a maintained production service), but reports are taken seriously and
triaged promptly.

## What's already covered

This repository runs its own security tooling on every pull request and does not rely on manual
review alone - see [`docs/security.md`](docs/security.md) for the full picture:

- Dependency vulnerability scanning for all three services (OWASP Dependency-Check, `pip-audit`,
  `npm audit`)
- Container image scanning (Trivy, blocking on HIGH/CRITICAL) before any image is pushed
- Infrastructure-as-code misconfiguration scanning (Trivy against the Terraform in
  `infrastructure/terraform`)
- Static analysis (CodeQL, across the Java backend, Python AI service, and TypeScript frontend)
- Secret scanning (gitleaks) on every pull request
- A documented threat model, authentication/authorisation design, and the reasoning behind every
  non-obvious security decision - see [`docs/decisions/`](docs/decisions)

## Supported versions

There's a single active line of development (`main`) - no older version receives security
patches separately.
