# Terraform module: monitoring

CloudWatch log groups for each ECS service's `awslogs` driver (required for the services to run
at all, not optional), plus a small set of infrastructure-level alarms (ALB 5xx, unhealthy
backend targets, low RDS free storage) into an SNS topic with no subscription yet. Wiring that
topic to email/Slack/PagerDuty, and any application-level dashboards, is Phase 14
(Observability & monitoring) scope, not built early to look more complete than it is.
