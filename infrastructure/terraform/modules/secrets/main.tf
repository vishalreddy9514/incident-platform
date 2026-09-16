locals {
  name_prefix = "${var.project}/${var.environment}"
}

# One JSON secret for the full DB connection, rather than one secret per
# field - it's what the ECS "secrets" container definition field and a
# human reading Secrets Manager both want: one coherent connection, not
# four unrelated-looking entries.
resource "aws_secretsmanager_secret" "db_credentials" {
  name        = "${local.name_prefix}/db-credentials"
  description = "RDS Postgres connection details for the backend."
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username = var.db_username
    password = var.db_password
    dbname   = var.db_name
  })
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name        = "${local.name_prefix}/jwt-secret"
  description = "HS256 signing key for backend-issued JWTs."
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = var.jwt_secret
}

resource "aws_secretsmanager_secret" "admin_bootstrap" {
  name        = "${local.name_prefix}/admin-bootstrap"
  description = "First ADMIN account credentials, read once by AdminBootstrapRunner on startup (ADR-0009)."
}

resource "aws_secretsmanager_secret_version" "admin_bootstrap" {
  secret_id = aws_secretsmanager_secret.admin_bootstrap.id
  secret_string = jsonencode({
    email    = var.admin_bootstrap_email
    password = var.admin_bootstrap_password
  })
}

resource "aws_secretsmanager_secret" "ai_service_internal_token" {
  name        = "${local.name_prefix}/ai-service-internal-token"
  description = "Shared token authenticating the backend's calls to the AI service's internal API."
}

resource "aws_secretsmanager_secret_version" "ai_service_internal_token" {
  secret_id     = aws_secretsmanager_secret.ai_service_internal_token.id
  secret_string = var.ai_service_internal_token
}

resource "aws_secretsmanager_secret" "llm_api_key" {
  name        = "${local.name_prefix}/llm-api-key"
  description = "Hosted LLM provider API key. Empty when LLM_PROVIDER=mock (ADR-0011)."
}

resource "aws_secretsmanager_secret_version" "llm_api_key" {
  secret_id     = aws_secretsmanager_secret.llm_api_key.id
  secret_string = var.llm_api_key
}
