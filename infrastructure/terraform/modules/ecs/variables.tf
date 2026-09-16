variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "backend_security_group_id" {
  type = string
}

variable "ai_service_security_group_id" {
  type = string
}

variable "frontend_security_group_id" {
  type = string
}

variable "execution_role_arn" {
  type = string
}

variable "backend_task_role_arn" {
  type = string
}

variable "ai_service_task_role_arn" {
  type = string
}

variable "frontend_task_role_arn" {
  type = string
}

variable "backend_target_group_arn" {
  type = string
}

variable "frontend_target_group_arn" {
  type = string
}

variable "backend_log_group_name" {
  type = string
}

variable "ai_service_log_group_name" {
  type = string
}

variable "frontend_log_group_name" {
  type = string
}

variable "aws_region" {
  type = string
}

# --- Images ---
# Tags default to "latest" so a first `terraform apply` (before any image
# has ever been pushed to these repos) doesn't need a real tag yet - but a
# real environment should always pin these to an immutable digest or SHA
# tag pushed by deploy.yml, not float on "latest" (see docs/architecture.md
# Phase 12 section and ADR-0012's GHCR-to-ECR migration note).
variable "backend_image_tag" {
  type    = string
  default = "latest"
}

variable "ai_service_image_tag" {
  type    = string
  default = "latest"
}

variable "frontend_image_tag" {
  type    = string
  default = "latest"
}

# --- Task sizing (Fargate cpu/memory units) ---
variable "backend_cpu" {
  description = "The JVM needs more headroom than the other two services."
  type        = number
  default     = 512
}

variable "backend_memory" {
  type    = number
  default = 1024
}

variable "ai_service_cpu" {
  type    = number
  default = 256
}

variable "ai_service_memory" {
  type    = number
  default = 512
}

variable "frontend_cpu" {
  type    = number
  default = 256
}

variable "frontend_memory" {
  type    = number
  default = 512
}

# --- Non-secret application configuration ---
variable "cors_allowed_origin" {
  type = string
}

variable "jwt_access_token_ttl_minutes" {
  type    = string
  default = "15"
}

variable "jwt_refresh_token_ttl_days" {
  type    = string
  default = "7"
}

variable "llm_provider" {
  type    = string
  default = "mock"
}

variable "llm_model" {
  type    = string
  default = ""
}

variable "db_host" {
  type = string
}

variable "db_port" {
  type = string
}

variable "redis_host" {
  type = string
}

variable "redis_port" {
  type = string
}

# --- Secrets Manager references ---
variable "db_credentials_secret_arn" {
  type = string
}

variable "jwt_secret_arn" {
  type = string
}

variable "admin_bootstrap_secret_arn" {
  type = string
}

variable "ai_service_internal_token_secret_arn" {
  type = string
}

variable "llm_api_key_secret_arn" {
  type = string
}

variable "desired_count" {
  description = "Fixed at 1 per service in dev - no autoscaling policy yet (Phase 14 territory), and cost-consciousness matters more than headroom for a portfolio environment."
  type        = number
  default     = 1
}
