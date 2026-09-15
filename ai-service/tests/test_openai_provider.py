import json

import httpx
import pytest

from app.config import Settings
from app.providers.errors import AnalysisProviderError
from app.providers.openai_provider import OpenAIAnalysisProvider
from app.schemas import AnalyseRequest


def _settings() -> Settings:
    return Settings(llm_provider="openai", llm_api_key="sk-test", llm_model="gpt-4o-mini")


def _fake_response(status_code: int, json_body: dict) -> httpx.Response:
    request = httpx.Request("POST", "https://api.openai.com/v1/chat/completions")
    return httpx.Response(status_code, json=json_body, request=request)


@pytest.fixture
def request_payload() -> AnalyseRequest:
    return AnalyseRequest(title="VPN down", description="Cannot connect to VPN from home.")


def test_constructor_requires_api_key():
    settings = Settings(llm_provider="openai", llm_api_key="")

    with pytest.raises(AnalysisProviderError):
        OpenAIAnalysisProvider(settings)


async def test_analyse_parses_valid_completion(monkeypatch, request_payload):
    completion = {
        "suggestedCategory": "Network",
        "predictedPriority": "HIGH",
        "summary": "User cannot connect to the VPN.",
        "keywords": ["vpn", "connection"],
        "suggestedSteps": ["Restart VPN client", "Check credentials"],
    }

    async def fake_post(self, url, **kwargs):
        body = {"choices": [{"message": {"content": json.dumps(completion)}}]}
        return _fake_response(200, body)

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    provider = OpenAIAnalysisProvider(_settings())
    result = await provider.analyse(request_payload)

    assert result.suggested_category == "Network"
    assert result.predicted_priority == "HIGH"
    assert result.model_used == "openai:gpt-4o-mini"


async def test_analyse_raises_on_non_200(monkeypatch, request_payload):
    async def fake_post(self, url, **kwargs):
        return _fake_response(500, {"error": "boom"})

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    provider = OpenAIAnalysisProvider(_settings())
    with pytest.raises(AnalysisProviderError):
        await provider.analyse(request_payload)


async def test_analyse_raises_on_malformed_content(monkeypatch, request_payload):
    async def fake_post(self, url, **kwargs):
        body = {"choices": [{"message": {"content": "not json"}}]}
        return _fake_response(200, body)

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    provider = OpenAIAnalysisProvider(_settings())
    with pytest.raises(AnalysisProviderError):
        await provider.analyse(request_payload)


async def test_analyse_raises_on_response_missing_required_field(monkeypatch, request_payload):
    async def fake_post(self, url, **kwargs):
        incomplete = {"suggestedCategory": "Network"}
        body = {"choices": [{"message": {"content": json.dumps(incomplete)}}]}
        return _fake_response(200, body)

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    provider = OpenAIAnalysisProvider(_settings())
    with pytest.raises(AnalysisProviderError):
        await provider.analyse(request_payload)


async def test_analyse_raises_on_network_error(monkeypatch, request_payload):
    async def fake_post(self, url, **kwargs):
        raise httpx.ConnectError("connection refused")

    monkeypatch.setattr(httpx.AsyncClient, "post", fake_post)

    provider = OpenAIAnalysisProvider(_settings())
    with pytest.raises(AnalysisProviderError):
        await provider.analyse(request_payload)
