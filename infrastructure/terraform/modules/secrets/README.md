# Terraform module: secrets

Secrets Manager entries for everything the running services need that shouldn't live in a task
definition's plain `environment` block: DB credentials, the JWT signing key, the admin-bootstrap
account, the backend/AI-service shared internal token, and the (optional — empty by default,
since `LLM_PROVIDER` defaults to `mock`, see ADR-0011) LLM API key. No values default to anything
real — supply them via `terraform.tfvars` (git-ignored) or `TF_VAR_*` environment variables.
