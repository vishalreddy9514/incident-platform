locals {
  name_prefix = "${var.project}-${var.environment}"
}

data "aws_iam_policy_document" "ecs_tasks_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

# One execution role shared by all three services: it only ever does two
# things (pull the image from ECR, write logs to CloudWatch, and - the
# reason it needs a per-environment policy at all - decrypt/read the
# Secrets Manager entries referenced in each task definition's "secrets"
# block). None of that is service-specific, unlike the task roles below.
resource "aws_iam_role" "ecs_execution" {
  name               = "${local.name_prefix}-ecs-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_execution_managed" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

data "aws_iam_policy_document" "ecs_execution_secrets" {
  statement {
    sid       = "ReadEnvironmentSecrets"
    actions   = ["secretsmanager:GetSecretValue"]
    resources = var.secret_arns
  }
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name   = "${local.name_prefix}-ecs-execution-secrets"
  role   = aws_iam_role.ecs_execution.id
  policy = data.aws_iam_policy_document.ecs_execution_secrets.json
}

# --- Per-service task roles ---
# These are the identity the *application code* runs as (as opposed to the
# execution role above, which only ECS agent machinery uses). None of the
# three services currently call any AWS API at runtime, so each role is
# deliberately left with no attached policy beyond assume-role - but they
# stay separate per service so that if, say, the backend later needs S3
# access for attachment storage, only backend_task_role changes. The
# AI service's task role in particular is expected to stay permission-free
# indefinitely: it never talks to Postgres or Secrets Manager directly.

resource "aws_iam_role" "backend_task" {
  name               = "${local.name_prefix}-backend-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role" "ai_service_task" {
  name               = "${local.name_prefix}-ai-service-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}

resource "aws_iam_role" "frontend_task" {
  name               = "${local.name_prefix}-frontend-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume_role.json
}
