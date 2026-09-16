terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Partial configuration on purpose: no bucket/table name (or account-
  # specific detail) is hard-coded into version control (Phase 1 §14's
  # "no hard-coded account IDs/ARNs in module code" principle, applied to
  # the backend block too). Supply the rest with:
  #   terraform init -backend-config=backend.hcl
  # where backend.hcl (git-ignored) is copied from backend.hcl.example and
  # points at the bucket/table the bootstrap config in ../../bootstrap
  # created.
  backend "s3" {
    key     = "dev/terraform.tfstate"
    encrypt = true
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

locals {
  cors_allowed_origin = "http://${module.load_balancer.dns_name}"
}

module "networking" {
  source = "../../modules/networking"

  project            = var.project
  environment        = var.environment
  availability_zones = var.availability_zones
}

module "secrets" {
  source = "../../modules/secrets"

  project     = var.project
  environment = var.environment

  db_username               = var.db_username
  db_password               = var.db_password
  db_name                   = var.db_name
  jwt_secret                = var.jwt_secret
  admin_bootstrap_email     = var.admin_bootstrap_email
  admin_bootstrap_password  = var.admin_bootstrap_password
  ai_service_internal_token = var.ai_service_internal_token
  llm_api_key               = var.llm_api_key
}

module "iam" {
  source = "../../modules/iam"

  project     = var.project
  environment = var.environment
  secret_arns = module.secrets.all_secret_arns
}

module "database" {
  source = "../../modules/database"

  project            = var.project
  environment        = var.environment
  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.rds_security_group_id
  db_name            = var.db_name
  db_username        = var.db_username
  db_password        = var.db_password
}

module "cache" {
  source = "../../modules/cache"

  project            = var.project
  environment        = var.environment
  private_subnet_ids = module.networking.private_subnet_ids
  security_group_id  = module.networking.redis_security_group_id
}

module "load_balancer" {
  source = "../../modules/load-balancer"

  project           = var.project
  environment       = var.environment
  vpc_id            = module.networking.vpc_id
  public_subnet_ids = module.networking.public_subnet_ids
  security_group_id = module.networking.alb_security_group_id
}

module "monitoring" {
  source = "../../modules/monitoring"

  project     = var.project
  environment = var.environment

  alb_arn_suffix                  = module.load_balancer.arn_suffix
  backend_target_group_arn_suffix = module.load_balancer.backend_target_group_arn_suffix
  db_instance_id                  = module.database.identifier
}

module "ecs" {
  source = "../../modules/ecs"

  project     = var.project
  environment = var.environment
  aws_region  = var.aws_region

  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids

  backend_security_group_id    = module.networking.backend_security_group_id
  ai_service_security_group_id = module.networking.ai_service_security_group_id
  frontend_security_group_id   = module.networking.frontend_security_group_id

  execution_role_arn       = module.iam.ecs_execution_role_arn
  backend_task_role_arn    = module.iam.backend_task_role_arn
  ai_service_task_role_arn = module.iam.ai_service_task_role_arn
  frontend_task_role_arn   = module.iam.frontend_task_role_arn

  backend_target_group_arn  = module.load_balancer.backend_target_group_arn
  frontend_target_group_arn = module.load_balancer.frontend_target_group_arn

  backend_log_group_name    = module.monitoring.backend_log_group_name
  ai_service_log_group_name = module.monitoring.ai_service_log_group_name
  frontend_log_group_name   = module.monitoring.frontend_log_group_name

  backend_image_tag    = var.backend_image_tag
  ai_service_image_tag = var.ai_service_image_tag
  frontend_image_tag   = var.frontend_image_tag

  cors_allowed_origin = local.cors_allowed_origin
  llm_provider        = var.llm_provider
  llm_model           = var.llm_model

  db_host    = module.database.endpoint
  db_port    = tostring(module.database.port)
  redis_host = module.cache.endpoint
  redis_port = tostring(module.cache.port)

  db_credentials_secret_arn            = module.secrets.db_credentials_arn
  jwt_secret_arn                       = module.secrets.jwt_secret_arn
  admin_bootstrap_secret_arn           = module.secrets.admin_bootstrap_arn
  ai_service_internal_token_secret_arn = module.secrets.ai_service_internal_token_arn
  llm_api_key_secret_arn               = module.secrets.llm_api_key_arn
}
