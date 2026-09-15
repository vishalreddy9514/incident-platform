from fastapi import FastAPI

from app.routers.analyse import router as analyse_router

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

app.include_router(analyse_router)
