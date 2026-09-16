locals {
  name_prefix = "${var.project}-${var.environment}"
}

resource "aws_db_subnet_group" "main" {
  name       = "${local.name_prefix}-db"
  subnet_ids = var.private_subnet_ids

  tags = { Name = "${local.name_prefix}-db-subnet-group" }
}

# Single-AZ, gp3, smallest Graviton instance class: matches Phase 1 §13's
# explicit "sized deliberately small - portfolio project, not a production
# workload" call, and Multi-AZ is one of that section's explicitly-not-used
# items (cost, not needed to demonstrate the skill).
resource "aws_db_instance" "main" {
  identifier     = "${local.name_prefix}-postgres"
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage = var.allocated_storage_gb
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false
  multi_az               = false

  backup_retention_period = var.backup_retention_days
  skip_final_snapshot     = true
  deletion_protection     = var.deletion_protection

  tags = { Name = "${local.name_prefix}-postgres" }
}
