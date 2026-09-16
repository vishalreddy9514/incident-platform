# GitHub Actions OIDC federation (ADR-0014): lets deploy.yml assume an AWS
# role by presenting a short-lived, repo/branch-scoped token GitHub itself
# issues - no AWS access key ever needs to be generated, stored as a
# GitHub secret, or rotated. Pure IAM (free to create) - not wired into
# deploy.yml yet, since that would need this actually applied against a
# real AWS account first (see docs/deployment.md's Phase 13 section).

variable "create_github_oidc_provider" {
  description = "False if this AWS account already has a token.actions.githubusercontent.com OIDC provider (only one may exist per account, regardless of how many repos/roles use it)."
  type        = bool
  default     = true
}

variable "github_org" {
  type    = string
  default = "vishalreddy9514"
}

variable "github_repo" {
  type    = string
  default = "incident-platform"
}

data "tls_certificate" "github_actions" {
  count = var.create_github_oidc_provider ? 1 : 0
  url   = "https://token.actions.githubusercontent.com/.well-known/openid-configuration"
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  count = var.create_github_oidc_provider ? 1 : 0

  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.github_actions[0].certificates[0].sha1_fingerprint]
}

locals {
  # Works whether this module created the provider or a pre-existing one is
  # being reused - either way the ARN has the same predictable shape.
  github_oidc_provider_arn = var.create_github_oidc_provider ? aws_iam_openid_connect_provider.github_actions[0].arn : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com"
}

data "aws_caller_identity" "current" {}

data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # Restricted to this repo's main branch specifically - deploy.yml only
    # ever runs on push to main (ADR-0005), so no other branch/PR run
    # needs, or should have, this role available to it.
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_org}/${var.github_repo}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_actions_deploy" {
  name               = "${local.name_prefix}-github-actions-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json
}

# Scoped to exactly what deploy.yml's image-push step needs today (ADR-0012's
# GHCR->ECR migration) - not a broad "terraform apply" role. Granting this
# workflow permission to change IAM/networking/database infrastructure by
# itself would be a much bigger trust decision than "let CI push images",
# and isn't something this project needs yet.
data "aws_iam_policy_document" "github_actions_deploy_ecr" {
  statement {
    sid       = "ECRAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "ECRPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
    ]
    resources = [
      "arn:aws:ecr:*:${data.aws_caller_identity.current.account_id}:repository/${var.project}-${var.environment}-*",
    ]
  }
}

resource "aws_iam_role_policy" "github_actions_deploy_ecr" {
  name   = "${local.name_prefix}-github-actions-deploy-ecr"
  role   = aws_iam_role.github_actions_deploy.id
  policy = data.aws_iam_policy_document.github_actions_deploy_ecr.json
}
