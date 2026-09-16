variable "project" {
  type = string
}

variable "environment" {
  type = string
}

variable "secret_arns" {
  description = "Secrets Manager ARNs the ECS execution role may read (to inject into containers as secrets)."
  type        = list(string)
}
