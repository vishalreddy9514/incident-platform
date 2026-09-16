# Deployment

> **Status:** Terraform (Phase 12) is written and validated but not yet applied — no AWS
> account is wired into this repository. Phase 13 (cloud deployment) covers actually running
> `terraform apply` against a real account, wiring `deploy.yml`'s image push over from GHCR to
> the ECR repositories this phase's Terraform creates, and the first end-to-end health check of a
> real deployment.
>
> Target architecture: [`PHASE-1-requirements-and-architecture.md`](../PHASE-1-requirements-and-architecture.md)
> §13-§14, and the "Terraform & AWS infrastructure (Phase 12)" section of
> [`architecture.md`](architecture.md).

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

### 3. Push real images and let the services stabilise

Until `deploy.yml` is switched from GHCR to these ECR repositories (Phase 13), push manually the
first time:

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
