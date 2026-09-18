# AI service application code

FastAPI app implementing the internal analysis endpoint from
`PHASE-1-requirements-and-architecture.md` §9.2 (Phase 8), plus observability (Phase 14).

```
app/
  main.py               FastAPI app instance, router registration, Prometheus instrumentation
  config.py             Settings (pydantic-settings), env-var backed
  schemas.py            AnalyseRequest/AnalyseResponse/HealthResponse (camelCase on the wire)
  security.py           Shared internal-token auth dependency
  logging_config.py     JSON console logging + the request-ID contextvar (Phase 14)
  middleware.py         Reads/echoes X-Request-Id, feeding logging_config's contextvar (Phase 14)
  routers/analyse.py    POST /internal/v1/analyse, GET /internal/v1/health
  providers/
    base.py             AnalysisProvider abstraction
    mock.py             Default: deterministic keyword-based heuristic, no external calls
    openai_provider.py  Real provider: calls OpenAI's Chat Completions API
    factory.py           Selects a provider from Settings.llm_provider
    errors.py           AnalysisProviderError -> mapped to HTTP 502 by the router
```

`GET /metrics` (Prometheus text format, via `prometheus-fastapi-instrumentator`) is also exposed
by `main.py` - see `observability/README.md` at the repository root for how it's scraped.

See ADR-0001 (why this is a separate service) and ADR-0011 (why the
provider is a runtime-selectable abstraction rather than one hard-coded
LLM integration) in `docs/decisions/`.

## Running locally

```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

Defaults to the mock provider — no API key needed. To use a real model,
set `LLM_PROVIDER=openai`, `LLM_API_KEY=...`, `LLM_MODEL=...` (see the
repository root `.env.example`).

```bash
ruff check .
black --check .
pytest
```
