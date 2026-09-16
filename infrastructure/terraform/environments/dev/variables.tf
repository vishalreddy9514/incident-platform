variable "aws_region" {
  type    = string
  default = "eu-west-2"
}

variable "project" {
  type    = string
  default = "incident-platform"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "availability_zones" {
  description = "Must be two real AZs in aws_region, e.g. [\"eu-west-2a\", \"eu-west-2b\"]."
  type        = list(string)
}

# --- Database ---
variable "db_name" {
  type    = string
  default = "incident_platform"
}

variable "db_username" {
  type    = string
  default = "incident_platform"
}

variable "db_password" {
  description = "No default - must be supplied via terraform.tfvars (git-ignored) or TF_VAR_db_password."
  type        = string
  sensitive   = true
}

# --- Secrets (no defaults - see terraform.tfvars.example) ---
variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "admin_bootstrap_email" {
  type    = string
  default = "admin@example.com"
}

variable "admin_bootstrap_password" {
  type      = string
  sensitive = true
}

variable "ai_service_internal_token" {
  type      = string
  sensitive = true
}

variable "llm_api_key" {
  type      = string
  sensitive = true
  default   = ""
}

variable "llm_provider" {
  type    = string
  default = "mock"
}

variable "llm_model" {
  type    = string
  default = ""
}

# --- Images ---
# All three default to "latest" so the first `terraform apply` (before any
# image has ever been pushed to the ECR repos this same apply creates)
# doesn't need a real tag yet - see the same note in modules/ecs/variables.tf.
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
