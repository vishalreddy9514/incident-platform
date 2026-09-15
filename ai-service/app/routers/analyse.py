"""The internal analysis API (FR-13/FR-14), matching
PHASE-1-requirements-and-architecture.md §9.2 exactly:

    POST /internal/v1/analyse   -> AnalyseResponse
    GET  /internal/v1/health    -> HealthResponse

Not exposed to the frontend directly (ADR-0001) — only the Java backend
calls this, authenticated with the shared internal token (app/security.py).
"""

from fastapi import APIRouter, Depends, HTTPException, status

from app.config import Settings, get_settings
from app.providers.base import AnalysisProvider
from app.providers.errors import AnalysisProviderError
from app.providers.factory import build_provider
from app.schemas import AnalyseRequest, AnalyseResponse, HealthResponse
from app.security import verify_internal_token

router = APIRouter(prefix="/internal/v1")


def get_provider(settings: Settings = Depends(get_settings)) -> AnalysisProvider:
    return build_provider(settings)


@router.get("/health", response_model=HealthResponse)
async def health(settings: Settings = Depends(get_settings)) -> HealthResponse:
    return HealthResponse(status="ok", provider=settings.llm_provider)


@router.post(
    "/analyse",
    response_model=AnalyseResponse,
    dependencies=[Depends(verify_internal_token)],
)
async def analyse(
    request: AnalyseRequest,
    provider: AnalysisProvider = Depends(get_provider),
) -> AnalyseResponse:
    try:
        return await provider.analyse(request)
    except AnalysisProviderError as exc:
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail=str(exc)) from exc
