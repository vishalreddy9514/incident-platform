# Terraform module: database

A single-AZ RDS Postgres instance (`db.t4g.micro`, gp3, encrypted at rest) plus its subnet group.
`deletion_protection = false` and `skip_final_snapshot = true` by default so `terraform destroy`
(the documented cost-avoidance workflow between demo sessions — see `docs/deployment.md`) works
without a manual console step first.
