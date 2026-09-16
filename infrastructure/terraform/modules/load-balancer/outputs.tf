output "dns_name" {
  value = aws_lb.main.dns_name
}

output "arn_suffix" {
  value = aws_lb.main.arn_suffix
}

output "frontend_target_group_arn" {
  value = aws_lb_target_group.frontend.arn
}

output "backend_target_group_arn" {
  value = aws_lb_target_group.backend.arn
}

output "backend_target_group_arn_suffix" {
  value = aws_lb_target_group.backend.arn_suffix
}

output "listener_arn" {
  value = aws_lb_listener.http.arn
}
