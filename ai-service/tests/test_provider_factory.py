import pytest

from app.config import Settings
from app.providers.errors import AnalysisProviderError
from app.providers.factory import build_provider
from app.providers.mock import MockAnalysisProvider
from app.providers.openai_provider import OpenAIAnalysisProvider


def test_build_provider_defaults_to_mock():
    settings = Settings(llm_provider="mock")

    assert isinstance(build_provider(settings), MockAnalysisProvider)


def test_build_provider_is_case_insensitive():
    settings = Settings(llm_provider="MOCK")

    assert isinstance(build_provider(settings), MockAnalysisProvider)


def test_build_provider_openai_requires_api_key():
    settings = Settings(llm_provider="openai", llm_api_key="")

    with pytest.raises(AnalysisProviderError):
        build_provider(settings)


def test_build_provider_openai_with_key():
    settings = Settings(llm_provider="openai", llm_api_key="sk-test", llm_model="gpt-4o-mini")

    assert isinstance(build_provider(settings), OpenAIAnalysisProvider)


def test_build_provider_rejects_unknown_provider():
    settings = Settings(llm_provider="bogus")

    with pytest.raises(ValueError):
        build_provider(settings)
