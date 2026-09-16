output "db_credentials_arn" {
  value = aws_secretsmanager_secret.db_credentials.arn
}

output "jwt_secret_arn" {
  value = aws_secretsmanager_secret.jwt_secret.arn
}

output "admin_bootstrap_arn" {
  value = aws_secretsmanager_secret.admin_bootstrap.arn
}

output "ai_service_internal_token_arn" {
  value = aws_secretsmanager_secret.ai_service_internal_token.arn
}

output "llm_api_key_arn" {
  value = aws_secretsmanager_secret.llm_api_key.arn
}

output "all_secret_arns" {
  description = "Every secret ARN this environment owns, for the IAM module's execution-role policy."
  value = [
    aws_secretsmanager_secret.db_credentials.arn,
    aws_secretsmanager_secret.jwt_secret.arn,
    aws_secretsmanager_secret.admin_bootstrap.arn,
    aws_secretsmanager_secret.ai_service_internal_token.arn,
    aws_secretsmanager_secret.llm_api_key.arn,
  ]
}
