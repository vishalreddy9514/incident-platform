variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "engine_version" {
  description = "Postgres major.minor version - kept in step with the postgres:16-alpine image used locally/in CI."
  type        = string
  default     = "16.4"
}

variable "instance_class" {
  type    = string
  default = "db.t4g.micro"
}

variable "allocated_storage_gb" {
  type    = number
  default = 20
}

variable "db_name" {
  type    = string
  default = "incident_platform"
}

variable "db_username" {
  type      = string
  sensitive = true
}

variable "db_password" {
  type      = string
  sensitive = true
}

variable "backup_retention_days" {
  description = "0 disables automated backups. Kept short (not the RDS default of 7) since this is a portfolio dev environment, not a workload with a real RPO."
  type        = number
  default     = 1
}

variable "deletion_protection" {
  description = "Left false in dev so `terraform destroy` (the documented cost-avoidance workflow between demo sessions) actually works without a manual console step first."
  type        = bool
  default     = false
}
