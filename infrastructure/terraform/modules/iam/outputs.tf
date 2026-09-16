output "ecs_execution_role_arn" {
  value = aws_iam_role.ecs_execution.arn
}

output "backend_task_role_arn" {
  value = aws_iam_role.backend_task.arn
}

output "ai_service_task_role_arn" {
  value = aws_iam_role.ai_service_task.arn
}

output "frontend_task_role_arn" {
  value = aws_iam_role.frontend_task.arn
}

output "github_actions_deploy_role_arn" {
  description = "Set as AWS_DEPLOY_ROLE_ARN in deploy.yml once ADR-0012's GHCR->ECR cutover happens."
  value       = aws_iam_role.github_actions_deploy.arn
}
