# Terraform module: ecs

ECR repositories, ECS cluster, task definitions, and services for all three application
containers, plus a Cloud Map private DNS namespace the backend uses to reach the AI service (the
ECS/Fargate equivalent of docker-compose's built-in service-name DNS — see
`docs/architecture.md`'s Phase 12 section). `desired_count` defaults to 1 per service; no
autoscaling policy yet (Phase 14 territory).
