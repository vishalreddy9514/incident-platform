Root Terraform configuration for the "dev" environment — wires every module in `../../modules`
together. See `docs/deployment.md` for the actual `terraform init`/`apply` walkthrough (bootstrap
first, then this), and `backend.hcl.example`/`terraform.tfvars.example` for what needs filling in
before it will run against a real AWS account.
