variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "log_retention_days" {
  type    = number
  default = 14
}

variable "alb_arn_suffix" {
  description = "ALB ARN suffix (the part CloudWatch metrics key on), e.g. app/name/id."
  type        = string
}

variable "backend_target_group_arn_suffix" {
  type = string
}

variable "db_instance_id" {
  type = string
}
