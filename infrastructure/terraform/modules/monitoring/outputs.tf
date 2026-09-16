output "backend_log_group_name" {
  value = aws_cloudwatch_log_group.backend.name
}

output "ai_service_log_group_name" {
  value = aws_cloudwatch_log_group.ai_service.name
}

output "frontend_log_group_name" {
  value = aws_cloudwatch_log_group.frontend.name
}

output "alerts_topic_arn" {
  value = aws_sns_topic.alerts.arn
}
