from app.config import Settings
from app.providers.base import AnalysisProvider
from app.providers.mock import MockAnalysisProvider
from app.providers.openai_provider import OpenAIAnalysisProvider


def build_provider(settings: Settings) -> AnalysisProvider:
    provider_name = settings.llm_provider.strip().lower()
    if provider_name == "mock":
        return MockAnalysisProvider()
    if provider_name == "openai":
        return OpenAIAnalysisProvider(settings)
    raise ValueError(
        f"Unsupported LLM_PROVIDER '{settings.llm_provider}' (expected 'mock' or 'openai')"
    )
