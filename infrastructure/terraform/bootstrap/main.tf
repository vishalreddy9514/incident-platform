# Provisions the S3 bucket + DynamoDB lock table that `environments/*`
# store their remote state in. Deliberately its own root module with local
# state (there's no remote backend to bootstrap a remote backend with) -
# run once, by hand, before `environments/dev` is initialised for the
# first time:
#
#   cd infrastructure/terraform/bootstrap
#   terraform init
#   terraform apply
#
# Its own state file (terraform.tfstate, git-ignored) then lives wherever
# it's run from - fine for a solo/portfolio context, where re-running this
# is a rare, deliberate action, not something that needs team-shared state
# of its own.

terraform {
  required_version = ">= 1.9"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_s3_bucket" "state" {
  bucket = var.state_bucket_name

  # Never destroyed by a routine `terraform destroy` of *this* config -
  # that would take every environment's state with it. Deleting this
  # bucket is a deliberate, separate action.
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket = aws_s3_bucket.state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "lock" {
  name         = var.lock_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  lifecycle {
    prevent_destroy = true
  }
}
