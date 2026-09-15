"""Internal service-to-service authentication.

The AI service is never called by the frontend directly (ADR-0001) — only
the Spring Boot backend, which is expected to send the shared secret from
its own ``app.ai-service.internal-token`` config as an ``X-Internal-Token``
header on every request to ``/internal/v1/analyse``.
"""

from fastapi import Depends, Header, HTTPException, status

from app.config import Settings, get_settings


async def verify_internal_token(
    x_internal_token: str | None = Header(default=None, alias="X-Internal-Token"),
    settings: Settings = Depends(get_settings),
) -> None:
    if not x_internal_token or x_internal_token != settings.ai_service_internal_token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing or invalid internal service token",
        )
