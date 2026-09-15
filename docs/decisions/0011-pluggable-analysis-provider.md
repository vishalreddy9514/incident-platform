# ADR-0011: Runtime-selectable analysis provider (mock default, real LLM optional)

**Status:** Accepted
**Date:** 2026-09-15
**Phase:** 8

## Context

The AI service (ADR-0001) needs to produce a suggested category, predicted priority, summary,
keywords, and suggested troubleshooting steps (FR-13) from incident text. Something has to
actually generate those fields, and how that "something" is chosen has real consequences for cost,
CI reliability, and how easy the service is to run locally.

## Options considered

1. **Hard-code a single real LLM provider** (e.g. always call OpenAI).
   - Pros: simplest code path; one thing to maintain.
   - Cons: every local run and every CI run needs a real API key and incurs real cost and
     network flakiness; `pytest` in CI would either need to mock the HTTP call anyway (in which
     case the "real" path is never actually exercised in CI) or spend real money on every push.
2. **Only a rule-based mock, no real-model option at all.**
   - Pros: zero cost, zero external dependency, deterministic and fast.
   - Cons: doesn't demonstrate calling a real hosted model at all, which is a reasonable thing to
     want to show in a portfolio project built partly to demonstrate AI integration skills.
3. **A provider abstraction (`AnalysisProvider`) selected at runtime via `LLM_PROVIDER`, with a
   deterministic mock as the default and a real provider (OpenAI, over plain `httpx`) as an
   opt-in.**
   - Pros: the mock provider needs no credentials and no network access, so it's the only one
     exercised in CI and the only one needed for `docker compose up` locally (matches FR-15's
     spirit of "the platform works without depending on an external AI call succeeding," applied
     one level down to the AI service's own dependency on its LLM backend); a real model is still
     one env var away (`LLM_PROVIDER=openai` + `LLM_API_KEY`) for anyone who wants to see it live;
     the abstraction means adding a second real provider later (Anthropic, etc.) is a new class
     implementing one `analyse()` method, not a rewrite.
   - Cons: two code paths to maintain instead of one; the mock's heuristics are simple keyword
     scoring, not a claim of triage accuracy — it exists to prove the contract and the end-to-end
     flow, not to be a good classifier.

## Decision

Implement `AnalysisProvider` as an abstract base with two implementations — `MockAnalysisProvider`
(default, heuristic, no external calls) and `OpenAIAnalysisProvider` (real, opt-in via
`LLM_PROVIDER=openai`) — selected by `app/providers/factory.py` from `Settings.llm_provider` at
request-dependency-resolution time. The router and everything above it depends only on the
abstract type.

## Consequences

- CI and local development never require an LLM API key or produce non-deterministic test
  failures from a real model call — the entire test suite runs against the mock provider.
- Choosing `LLM_PROVIDER=openai` without `LLM_API_KEY` set fails fast with a clear
  `AnalysisProviderError` at the point the provider is constructed, not as a confusing runtime
  error deep inside a request.
- A genuinely broken or slow real-provider call surfaces to the backend as `502 Bad Gateway`
  (`AnalysisProviderError` mapped in `app/routers/analyse.py`), which is distinct from the AI
  service being unreachable altogether — the backend's FR-15 graceful-degradation handling can
  treat these differently if it chooses to.
- This decision is scoped to *one* real provider (OpenAI) for now, not several — adding another
  is a follow-up, not a redesign, but isn't built speculatively ahead of an actual need.
