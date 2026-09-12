# ADR-0004: Deploy containers on ECS Fargate, not EKS

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 1

## Context

The platform has three containerised services (backend, ai-service, frontend) that need to run in AWS with load balancing, health checks, and IAM-scoped access to other AWS resources (RDS, ElastiCache, Secrets Manager). This is a personal portfolio project with two competing goals: demonstrate real container-orchestration and cloud-deployment skill, and stay within a realistic personal-AWS-account budget and complexity level (Phase 1 NFR: cost; explicit non-goal: "do not introduce unnecessary AWS services just to make the project look complicated").

## Options considered

1. **Amazon EKS (managed Kubernetes).**
   - Pros: Kubernetes is extremely widely used in industry; deep orchestration features (custom schedulers, operators, service mesh potential).
   - Cons: EKS charges a flat per-cluster control-plane fee (in addition to node costs) regardless of workload size — a meaningful ongoing cost for a project with three small services and no team to amortise it across. Also a significant operational surface (node group management, cluster upgrades, Kubernetes-specific IAM via IRSA) that this project's scope doesn't need to justify.
2. **Amazon ECS on Fargate.**
   - Pros: real container orchestration (task definitions, service auto-recovery, ALB integration, IAM task roles) without managing a control plane or worker nodes; no flat cluster fee — pay only for the vCPU/memory the running tasks actually use; significantly simpler to reason about and tear down between demo sessions.
   - Cons: less transferable to teams specifically standardised on Kubernetes; fewer advanced orchestration features (acceptable — this project doesn't need them).
3. **EC2 with manually managed Docker** (no orchestration layer).
   - Rejected: wouldn't demonstrate orchestration skill at all, and patching/managing raw EC2 instances is exactly the operational burden managed container services exist to remove.

## Decision

Deploy all three services on Amazon ECS with the Fargate launch type, fronted by an Application Load Balancer.

## Consequences

- Meaningfully lower ongoing cost than EKS for this project's scale, and easy to tear down (`terraform destroy`) and recreate for demos — directly supporting the cost NFR.
- Still demonstrates genuine, explainable cloud-native skills: task definitions, service definitions, IAM task roles (least privilege per service), ALB target groups/health checks, ECR image deployment.
- If Kubernetes experience specifically becomes a priority later (e.g. a job description explicitly requires it), that's a legitimate, separately-scoped follow-up project rather than a reason to retrofit this one — noted as a future improvement, not solved here.
