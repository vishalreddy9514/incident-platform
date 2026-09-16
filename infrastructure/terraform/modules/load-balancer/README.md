# Terraform module: load-balancer

An internet-facing ALB with a single HTTP listener: `/api/*` forwards to the backend target
group, everything else to the frontend target group. No HTTPS/TLS yet — see ADR-0013 for why (no
domain exists to issue a certificate against).
