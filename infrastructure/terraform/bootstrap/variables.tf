variable "aws_region" {
  type    = string
  default = "eu-west-2"
}

variable "state_bucket_name" {
  description = "Must be globally unique - the default embeds no account ID, so override it before applying (e.g. with a random or account-scoped suffix)."
  type        = string
  default     = "incident-platform-terraform-state"
}

variable "lock_table_name" {
  type    = string
  default = "incident-platform-terraform-locks"
}
