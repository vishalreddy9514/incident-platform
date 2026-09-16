locals {
  name_prefix = "${var.project}-${var.environment}"
}

# --- ECR ---
# One repository per service, scanned on push. No lifecycle policy trimming
# old images yet - a real gap, deferred alongside the GHCR->ECR cutover
# itself (ADR-0012/ADR-0013), not worth solving before anything is even
# pushed here.
resource "aws_ecr_repository" "backend" {
  name                 = "${local.name_prefix}-backend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "ai_service" {
  name                 = "${local.name_prefix}-ai-service"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "frontend" {
  name                 = "${local.name_prefix}-frontend"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# --- Cluster ---
resource "aws_ecs_cluster" "main" {
  name = local.name_prefix

  # Container Insights left off: it's a small but real per-metric
  # CloudWatch cost, and the monitoring module's log groups + ALB/RDS
  # alarms already cover what this dev environment needs.
  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

# --- Service discovery (the AI service is never reachable via the ALB;
# the backend finds it through Cloud Map instead, the ECS/Fargate
# equivalent of docker-compose's built-in DNS resolving "ai-service") ---
resource "aws_service_discovery_private_dns_namespace" "internal" {
  name = "${local.name_prefix}.internal"
  vpc  = var.vpc_id
}

resource "aws_service_discovery_service" "ai_service" {
  name = "ai-service"

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.internal.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  # Present so the ECS service (via service_registries) drives this
  # service's Cloud Map health status itself; failure_threshold is AWS-
  # deprecated and always 1 now, so it's omitted rather than set.
  health_check_custom_config {}
}

# --- Task definitions ---

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.backend_cpu
  memory                   = var.backend_memory
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.backend_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
      essential = true
      portMappings = [
        { containerPort = 8080, protocol = "tcp" }
      ]
      environment = [
        { name = "POSTGRES_HOST", value = var.db_host },
        { name = "POSTGRES_PORT", value = var.db_port },
        { name = "POSTGRES_DB", value = "incident_platform" },
        { name = "REDIS_HOST", value = var.redis_host },
        { name = "REDIS_PORT", value = var.redis_port },
        { name = "BACKEND_PORT", value = "8080" },
        { name = "CORS_ALLOWED_ORIGIN", value = var.cors_allowed_origin },
        { name = "JWT_ACCESS_TOKEN_TTL_MINUTES", value = var.jwt_access_token_ttl_minutes },
        { name = "JWT_REFRESH_TOKEN_TTL_DAYS", value = var.jwt_refresh_token_ttl_days },
        { name = "AI_SERVICE_BASE_URL", value = "http://ai-service.${local.name_prefix}.internal:8000" },
      ]
      secrets = [
        { name = "POSTGRES_USER", valueFrom = "${var.db_credentials_secret_arn}:username::" },
        { name = "POSTGRES_PASSWORD", valueFrom = "${var.db_credentials_secret_arn}:password::" },
        { name = "JWT_SECRET", valueFrom = var.jwt_secret_arn },
        { name = "ADMIN_BOOTSTRAP_EMAIL", valueFrom = "${var.admin_bootstrap_secret_arn}:email::" },
        { name = "ADMIN_BOOTSTRAP_PASSWORD", valueFrom = "${var.admin_bootstrap_secret_arn}:password::" },
        { name = "AI_SERVICE_INTERNAL_TOKEN", valueFrom = var.ai_service_internal_token_secret_arn },
      ]
      # No container-level healthCheck override: the image's own
      # HEALTHCHECK (backend/Dockerfile) is honoured by Fargate directly,
      # so the command isn't duplicated (and can't drift) here.
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = var.backend_log_group_name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "ai_service" {
  family                   = "${local.name_prefix}-ai-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.ai_service_cpu
  memory                   = var.ai_service_memory
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.ai_service_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "ai-service"
      image     = "${aws_ecr_repository.ai_service.repository_url}:${var.ai_service_image_tag}"
      essential = true
      portMappings = [
        { containerPort = 8000, protocol = "tcp" }
      ]
      environment = [
        { name = "LLM_PROVIDER", value = var.llm_provider },
        { name = "LLM_MODEL", value = var.llm_model },
      ]
      secrets = [
        { name = "AI_SERVICE_INTERNAL_TOKEN", valueFrom = var.ai_service_internal_token_secret_arn },
        { name = "LLM_API_KEY", valueFrom = var.llm_api_key_secret_arn },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = var.ai_service_log_group_name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${local.name_prefix}-frontend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.frontend_task_role_arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = "${aws_ecr_repository.frontend.repository_url}:${var.frontend_image_tag}"
      essential = true
      portMappings = [
        { containerPort = 80, protocol = "tcp" }
      ]
      # No VITE_API_BASE_URL env var here - it's baked into the static
      # build at image-build time, not read at container runtime (see
      # docs/architecture.md Phase 12 section for the resulting two-step
      # first-apply/rebuild workflow this implies).
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = var.frontend_log_group_name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# --- Services ---

resource "aws_ecs_service" "backend" {
  name            = "${local.name_prefix}-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.backend_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.backend_target_group_arn
    container_name   = "backend"
    container_port   = 8080
  }
}

resource "aws_ecs_service" "ai_service" {
  name            = "${local.name_prefix}-ai-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.ai_service.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.ai_service_security_group_id]
    assign_public_ip = false
  }

  service_registries {
    registry_arn = aws_service_discovery_service.ai_service.arn
  }
}

resource "aws_ecs_service" "frontend" {
  name            = "${local.name_prefix}-frontend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [var.frontend_security_group_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.frontend_target_group_arn
    container_name   = "frontend"
    container_port   = 80
  }
}
