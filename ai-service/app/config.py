"""Environment-backed configuration for the AI service.

Field names map to environment variables by uppercasing (pydantic-settings
default), so these line up directly with the ``AI_SERVICE_*``/``LLM_*``
variables documented in the repository root ``.env.example``.
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ai_service_port: int = 8000

    # Shared secret the Spring Boot backend sends on every internal call
    # (see docs/architecture.md §10 "service-to-service auth" and
    # app.ai-service.internal-token in backend/src/main/resources/application.yml).
    ai_service_internal_token: str = "change-me-shared-internal-token"

    # Selects the AnalysisProvider implementation (see app/providers/factory.py).
    # "mock" needs no external credentials or network access and is the safe
    # default for local development and CI; "openai" calls a real hosted model.
    llm_provider: str = "mock"
    llm_api_key: str = ""
    llm_model: str = "gpt-4o-mini"


@lru_cache
def get_settings() -> Settings:
    return Settings()
