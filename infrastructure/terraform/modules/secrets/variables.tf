variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_password" {
  description = "RDS master password. Supply via TF_VAR_db_password or a gitignored tfvars file - never commit a real value."
  type        = string
  sensitive   = true
}

variable "db_name" {
  type    = string
  default = "incident_platform"
}

variable "jwt_secret" {
  description = "HS256 signing key for backend-issued JWTs, at least 32 bytes."
  type        = string
  sensitive   = true
}

variable "admin_bootstrap_email" {
  description = "Email for the auto-created first ADMIN account. Empty string disables bootstrap (see ADR-0009)."
  type        = string
  default     = ""
}

variable "admin_bootstrap_password" {
  type      = string
  sensitive = true
  default   = ""
}

variable "ai_service_internal_token" {
  description = "Shared secret the backend and AI service use to authenticate the backend's internal calls."
  type        = string
  sensitive   = true
}

variable "llm_api_key" {
  description = "Hosted LLM provider API key. Empty string is valid - the AI service defaults to LLM_PROVIDER=mock and never reads this (ADR-0011)."
  type        = string
  sensitive   = true
  default     = ""
}
