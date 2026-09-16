# Terraform module: networking

VPC, two public + two private subnets (one pair per AZ), a single NAT gateway, route tables, and
the security-group chain everything else attaches to: `alb -> {frontend, backend} -> {ai_service,
rds, redis}`. The AI service's security group has no ingress from the ALB and nothing routes to
`rds`/`redis` except the backend — see `docs/architecture.md`'s Phase 12 section for why.
