output "cluster_name" {
  value = aws_ecs_cluster.main.name
}

output "backend_repository_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "ai_service_repository_url" {
  value = aws_ecr_repository.ai_service.repository_url
}

output "frontend_repository_url" {
  value = aws_ecr_repository.frontend.repository_url
}

output "backend_service_name" {
  value = aws_ecs_service.backend.name
}

output "ai_service_service_name" {
  value = aws_ecs_service.ai_service.name
}

output "frontend_service_name" {
  value = aws_ecs_service.frontend.name
}
