# Observability (Phase 14)

Self-hosted Prometheus + Grafana via `docker-compose.yml`, per Phase 1 §13: CloudWatch (Phase 12's
Terraform) covers base infrastructure metrics/logs in AWS; this layer covers the richer
application-level dashboards, without the cost of AWS Managed Prometheus/Grafana.

```
observability/
├── prometheus/
│   ├── prometheus.yml     # scrape config: backend + ai-service + itself
│   └── alert-rules.yml    # alerting rules, evaluated by Prometheus itself
└── grafana/
    └── provisioning/
        ├── datasources/   # auto-wires the Prometheus datasource on startup
        └── dashboards/    # auto-loads the starter dashboard below
```

## What's scraped

- **Backend**: `GET /actuator/prometheus` (Micrometer, already on the classpath - see
  `backend/pom.xml`'s Observability section). Standard Spring Boot metric names:
  `http_server_requests_seconds_{count,bucket,sum}`, `jvm_memory_used_bytes`, etc.
- **AI service**: `GET /metrics` (`prometheus-fastapi-instrumentator`, wired in `app/main.py`).
  Standard instrumentator metric names: `http_requests_total`, `http_request_duration_seconds_*`.

Both endpoints are intentionally unauthenticated (see the comments next to
`SecurityConfig.filterChain` and `app/main.py`) - they carry no secrets, and neither is reachable
from outside each service's own Docker network (or, in `infrastructure/terraform`, its own VPC
security group).

## Structured logging and request correlation

Both services emit one JSON log line per event (`backend/src/main/resources/logback-spring.xml`,
`ai-service/app/logging_config.py`) instead of Spring Boot/uvicorn's default plain-text console
output - the same format CloudWatch Logs (Phase 12) or any other aggregator can parse without a
regex. `RequestCorrelationFilter` (backend) puts an `X-Request-Id` in the logging MDC for every
request and forwards it on the backend's own call to the AI service
(`AiAnalysisClient`/`app/middleware.py`), so one incident's AI-analysis request can be traced
across both services' logs by grepping a single `requestId` field - reusing an inbound
`X-Request-Id` if one was already set, rather than always minting a fresh one, so a trace started
upstream (e.g. an ALB) isn't broken.

## Alerting

`prometheus/alert-rules.yml` defines real rules (service down, high 5xx rate, high p95 latency,
high JVM heap usage) that Prometheus evaluates continuously and shows under its own "Alerts" UI
(`http://localhost:9090/alerts`) regardless of whether anything is subscribed to act on them.

**No Alertmanager is wired up** - the same documented-gap pattern as
`infrastructure/terraform/modules/monitoring`'s CloudWatch SNS topic (created, no subscription):
routing a firing alert to a person needs a real destination (Slack webhook, email, PagerDuty),
none of which exist for this portfolio project. The rules themselves are real and demonstrably
evaluated; wiring notifications to an actual destination is future work, not faked here.

## Running it

```bash
docker compose up --build
```

- Prometheus: `http://localhost:9090` (targets: `/targets`, rules: `/rules`, alerts: `/alerts`)
- Grafana: `http://localhost:3000` (login via `GRAFANA_ADMIN_USER`/`GRAFANA_ADMIN_PASSWORD` in
  `.env`, defaults `admin`/`change-me-locally`) - the "Incident Platform" dashboard is provisioned
  automatically, no manual setup needed.
