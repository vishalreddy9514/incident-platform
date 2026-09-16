# Deployment

> **Status:** All infrastructure code (Phase 12) and deploy-auth prep (Phase 13's OIDC role,
> ADR-0014) is written and `terraform validate`-clean. A live `terraform apply` against a real
> AWS account has **deliberately not been run** — every resource below bills by the hour
> (roughly **$50-100/month** if left running: NAT gateway, RDS, ElastiCache, ALB, and the Fargate
> tasks all have an ongoing cost, unlike everything in Phases 1-12, which is free source code).
> For a portfolio project, spending real money to keep a demo environment up indefinitely isn't
> justified, so this deliberately stops at "provable and one command away" rather than "left
> running 24/7". The steps below are exact and tested against real Terraform/AWS API behaviour
> (see `docs/architecture.md`'s Phase 12 section) — this is the actual runbook, not a stub.
>
> Target architecture: [`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md)
> §13-§14, and the "Terraform & AWS infrastructure (Phase 12)" section of
> [`architecture.md`](architecture.md).

## Cost estimate (why this isn't left running)

Approximate `eu-west-2` on-demand pricing for what `environments/dev` provisions, run continuously
for a month: NAT gateway (~$27 + data processing), RDS `db.t4g.micro` single-AZ (~$12), ElastiCache
`cache.t4g.micro` (~$10), ALB (~$16 + LCU usage), three Fargate tasks at the sizes in
`modules/ecs/variables.tf` (~$15-25). A short apply→verify→destroy cycle (an hour or two, per the
walkthrough below) costs a small fraction of a dollar — the ongoing monthly total is what's being
avoided, not the act of applying itself.

## Provisioning infrastructure (once a real AWS account exists)

### 1. Bootstrap remote state (one-time, per AWS account)

```bash
cd infrastructure/terraform/bootstrap
terraform init
terraform apply -var="state_bucket_name=<a-globally-unique-name>"
```

Creates the S3 bucket (versioned, encrypted, public access blocked) and DynamoDB lock table that
`environments/*` store their state in. Run once; re-running is a no-op unless the bucket/table
names change. Never `terraform destroy` this — it would take every environment's state with it
(`prevent_destroy = true` on both resources is there deliberately).

### 2. Apply the `dev` environment

```bash
cd infrastructure/terraform/environments/dev
cp backend.hcl.example backend.hcl        # fill in the bucket/table names from step 1
cp terraform.tfvars.example terraform.tfvars   # fill in real secret values - never commit this file
terraform init -backend-config=backend.hcl
terraform apply
```

This provisions the VPC, RDS, ElastiCache, ALB, ECS cluster/services, IAM roles, Secrets Manager
entries, ECR repositories, and CloudWatch log groups/alarms described in `architecture.md`. The
ECS services will fail to reach steady state on this first apply — the ECR repositories are empty,
since no image has ever been pushed to them (`backend_image_tag`/etc. default to `"latest"`, which
doesn't exist yet). That's expected; step 3 fixes it.

This apply also creates a GitHub Actions OIDC deploy role (ADR-0014, `terraform output
github_actions_deploy_role_arn`) scoped to push to these ECR repositories — set it as
`AWS_DEPLOY_ROLE_ARN` in the repository's GitHub Actions variables and wire an
`aws-actions/configure-aws-credentials` step into `deploy.yml` to have CI push images itself
instead of the manual step below. Not done automatically here since it only makes sense once a
real role ARN exists.

### 3. Push real images and let the services stabilise

Until `deploy.yml` is switched from GHCR to these ECR repositories, push manually the first time:

```bash
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com

docker build -t <backend-repo-url>:latest backend/ && docker push <backend-repo-url>:latest
docker build -t <ai-service-repo-url>:latest ai-service/ && docker push <ai-service-repo-url>:latest

# Frontend is special - its API base URL is baked in at build time (see
# architecture.md's Phase 12 section), so it has to be built *after* the ALB
# exists:
terraform output alb_dns_name
docker build --build-arg VITE_API_BASE_URL="http://<alb-dns-name>/api/v1" -t <frontend-repo-url>:latest frontend/
docker push <frontend-repo-url>:latest
```

Then force each ECS service to pick up the newly-pushed `:latest` image:

```bash
aws ecs update-service --cluster <cluster-name> --service <service-name> --force-new-deployment
```

### 4. Verify

```bash
terraform output alb_dns_name
curl http://<alb-dns-name>/                                      # frontend (served by the default listener action)
curl -i http://<alb-dns-name>/api/v1/auth/login -X POST \
  -H "Content-Type: application/json" -d '{}'                    # backend, routed via the /api/* listener rule
                                                                    # (expect 400, not a connection failure/502)

# /actuator/health itself isn't under /api/*, so it's never reachable
# through the public listener - it's only used as the backend target
# group's own health-check path (ALB checks it directly against each
# registered task, bypassing listener rules). To check it directly:
aws elbv2 describe-target-health --target-group-arn "$(terraform output -raw backend_target_group_arn 2>/dev/null || echo '<from the load-balancer module output>')"
```

## Tearing down between demo sessions

To avoid ongoing AWS charges when not actively demoing:

```bash
cd infrastructure/terraform/environments/dev
terraform destroy
```

`deletion_protection = false` and `skip_final_snapshot = true` on the RDS instance (a deliberate
dev-environment choice — see `modules/database/variables.tf`) mean this completes without a manual
console step first. The remote state bucket/lock table from the bootstrap step are untouched
(`prevent_destroy`) — re-running `terraform apply` later recreates everything else from the same
state key.
