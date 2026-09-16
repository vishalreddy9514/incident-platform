# ADR-0013: HTTP-only ALB now, TLS deferred until a real domain exists

**Status:** Accepted
**Date:** 2026-09-16
**Phase:** 12

## Context

Phase 1 §13 describes the ALB doing "TLS termination" as part of its role. An ACM certificate,
though, has to be issued against a real domain name that the requester can prove they control
(DNS validation, or email validation) - and this project doesn't own a domain. Terraform can
create an ALB, target groups, and a listener without one; it cannot conjure a certificate for a
domain that doesn't exist.

## Options considered

1. **Request an ACM certificate for a placeholder/example domain anyway.** Rejected outright -
   ACM won't issue a publicly-trusted certificate without proving control of the domain, so this
   either fails at `apply` time or requires a fake self-signed certificate that satisfies nothing
   real and would need special-casing in Terraform to import.
2. **Register a real domain now, just to have one.** Rejected for this phase - Phase 1 explicitly
   scoped this project to what a solo portfolio build can run and tear down between demo sessions;
   a domain is an ongoing cost and commitment that belongs with the actual cloud deployment
   decision (Phase 13), not bundled into "does the Terraform apply cleanly."
3. **ALB listens on HTTP (port 80) only for now; add HTTPS once Phase 13 picks a domain.**
   Chosen. The listener/target-group/routing logic Phase 12 needs to prove out (path-based
   routing to the frontend vs. backend target groups) is identical whether the front door is
   HTTP or HTTPS - only the listener protocol and an extra `aws_acm_certificate` +
   `aws_route53_record` (if Route53 hosts the domain) change later.

## Decision

`infrastructure/terraform/modules/load-balancer` provisions the ALB with a single HTTP listener
on port 80, forwarding to the frontend target group by default and to the backend target group
for `/api/*`. No ACM certificate, no HTTPS listener, no Route53 records exist yet.

## Consequences

- Everything Phase 12 needs to demonstrate (VPC, ECS/Fargate, RDS, ElastiCache, ALB routing, IAM
  least privilege, Secrets Manager, remote state) is real and `terraform validate`-clean today,
  with nothing faked to route around the missing domain.
- Traffic to the ALB is unencrypted until Phase 13. Acceptable for a `dev` environment behind no
  real users yet; documented here and in `docs/deployment.md` rather than silently shipped.
- Phase 13's concrete follow-up: pick/register a domain, add `aws_acm_certificate` (DNS-validated),
  an HTTPS listener (443) with that certificate, a redirect from 80→443, and - if Route53 hosts the
  domain - an alias record pointing at the ALB. This ADR's HTTP listener stays as the redirect
  target, not replaced.
