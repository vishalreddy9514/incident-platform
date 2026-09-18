from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

from app.logging_config import configure_logging
from app.middleware import request_correlation_middleware
from app.routers.analyse import router as analyse_router

configure_logging()

app = FastAPI(
    title="Incident Platform AI Service",
    description=(
        "Internal AI-assisted incident analysis (classification, summarisation, "
        "keyword extraction, suggested troubleshooting steps). Advisory only "
        "(FR-14) and called only by the Spring Boot backend, never the frontend "
        "directly (ADR-0001)."
    ),
    version="0.1.0",
)

app.middleware("http")(request_correlation_middleware)
app.include_router(analyse_router)

# Exposes GET /metrics in Prometheus text format (request count/latency by
# path+status, unauthenticated for the same reason as the backend's
# /actuator/prometheus - see SecurityConfig - it's never reachable outside
# this service's own network, and carries no secrets).
Instrumentator().instrument(app).expose(app)
