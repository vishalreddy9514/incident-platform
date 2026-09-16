locals {
  name_prefix = "${var.project}-${var.environment}"
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.name_prefix}-redis"
  subnet_ids = var.private_subnet_ids
}

# A single-node cluster (aws_elasticache_cluster), not a replication group:
# no failover/read-replica requirement exists here (Redis is used for
# caching/rate-limiting, not as a source of truth - see docs/architecture.md)
# and a replication group's extra primary+replica cost isn't justified for
# a portfolio dev environment. Matches Phase 1 §13's "smallest node type".
resource "aws_elasticache_cluster" "main" {
  cluster_id         = "${local.name_prefix}-redis"
  engine             = "redis"
  engine_version     = var.engine_version
  node_type          = var.node_type
  num_cache_nodes    = 1
  port               = 6379
  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [var.security_group_id]

  tags = { Name = "${local.name_prefix}-redis" }
}
