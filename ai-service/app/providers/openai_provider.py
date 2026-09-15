"""Real-model analysis provider, calling OpenAI's Chat Completions API.

Implemented over plain ``httpx`` (already a dependency for the app itself)
rather than the ``openai`` SDK, to avoid a second HTTP client dependency
for a single JSON-in/JSON-out call. Selected via ``LLM_PROVIDER=openai``
(see ADR-0011); disabled by default so CI/local dev never needs a real key.
"""

import json

import httpx
from pydantic import ValidationError

from app.config import Settings
from app.providers.base import AnalysisProvider
from app.providers.errors import AnalysisProviderError
from app.schemas import AnalyseRequest, AnalyseResponse

CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"

SYSTEM_PROMPT = (
    "You are an incident triage assistant for an internal IT service desk. "
    "Given an incident title and description, respond with ONLY a single JSON "
    "object (no prose, no markdown fences) with exactly these keys: "
    'suggestedCategory (one of "Hardware", "Software", "Network", '
    '"Access & Permissions", "Database", "Security", "Other"), '
    'predictedPriority (one of "LOW", "MEDIUM", "HIGH", "CRITICAL"), '
    "summary (a concise one-to-two sentence summary), "
    "keywords (a JSON array of 3-8 short lowercase keyword strings), "
    "suggestedSteps (a JSON array of 3-5 short suggested troubleshooting steps)."
)


class OpenAIAnalysisProvider(AnalysisProvider):
    def __init__(self, settings: Settings, timeout_seconds: float = 20.0) -> None:
        if not settings.llm_api_key:
            raise AnalysisProviderError(
                "LLM_API_KEY is not configured, but LLM_PROVIDER=openai was selected"
            )
        self._api_key = settings.llm_api_key
        self._model = settings.llm_model
        self._timeout_seconds = timeout_seconds

    async def analyse(self, request: AnalyseRequest) -> AnalyseResponse:
        user_content = (
            f"Title: {request.title}\n"
            f"Description: {request.description}\n"
            f"Existing category (optional hint): {request.category or 'none'}"
        )
        payload = {
            "model": self._model,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_content},
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.2,
        }
        headers = {"Authorization": f"Bearer {self._api_key}"}

        try:
            async with httpx.AsyncClient(timeout=self._timeout_seconds) as client:
                response = await client.post(CHAT_COMPLETIONS_URL, json=payload, headers=headers)
        except httpx.HTTPError as exc:
            raise AnalysisProviderError(f"Failed to reach OpenAI: {exc}") from exc

        if response.status_code != 200:
            raise AnalysisProviderError(
                f"OpenAI returned HTTP {response.status_code}: {response.text[:500]}"
            )

        try:
            content = response.json()["choices"][0]["message"]["content"]
            parsed = json.loads(content)
            # The model is only asked to produce the analysis fields (see
            # SYSTEM_PROMPT) — modelUsed is our own bookkeeping, added here
            # rather than validated as part of its output.
            parsed["modelUsed"] = f"openai:{self._model}"
            result = AnalyseResponse.model_validate(parsed)
        except (KeyError, IndexError, TypeError, json.JSONDecodeError, ValidationError) as exc:
            raise AnalysisProviderError(f"Could not parse OpenAI response: {exc}") from exc

        return result
