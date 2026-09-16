output "alb_dns_name" {
  value = module.load_balancer.dns_name
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "backend_target_group_arn" {
  value = module.load_balancer.backend_target_group_arn
}

output "backend_ecr_repository_url" {
  value = module.ecs.backend_repository_url
}

output "ai_service_ecr_repository_url" {
  value = module.ecs.ai_service_repository_url
}

output "frontend_ecr_repository_url" {
  value = module.ecs.frontend_repository_url
}

output "rds_endpoint" {
  value     = module.database.endpoint
  sensitive = true
}

output "redis_endpoint" {
  value     = module.cache.endpoint
  sensitive = true
}
