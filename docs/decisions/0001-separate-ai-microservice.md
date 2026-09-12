# ADR-0001: Run AI analysis as a separate Python microservice, not embedded in the Java backend

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 1

## Context

The platform needs AI-assisted incident analysis (classification, priority prediction, summarisation, keyword extraction, suggested troubleshooting steps). This could live as a module inside the Spring Boot backend (calling an LLM provider directly from Java) or as a genuinely separate service.

The core incident workflow (FR-15) must keep working if AI analysis is unavailable — this is a hard functional requirement, not a nice-to-have.

## Options considered

1. **Embedded in the Java backend** — a service class in Spring Boot calling the LLM provider directly.
   - Pros: one codebase, one deployment, no network hop.
   - Cons: couples the AI provider SDK/ecosystem (best supported in Python) into the Java app; an AI-related failure or slow response risks affecting the core request thread pool; doesn't demonstrate microservice/polyglot skills.
2. **Separate Python (FastAPI) microservice, called over REST by the backend.**
   - Pros: uses the language with the strongest LLM tooling; failure isolation is structural (a separate process, not a try/catch); independently scalable/deployable; demonstrates a genuine second service with its own contract, tests, and pipeline.
   - Cons: extra network hop and latency; two codebases and two sets of tooling to maintain; requires an internal auth mechanism between services.
3. **Third-party AI platform embedded via frontend SDK** (calling an LLM directly from the browser).
   - Rejected outright: would expose API keys client-side and bypass all backend authorisation — a non-starter on security grounds alone.

## Decision

Build the AI analysis capability as a separate FastAPI service (`ai-service/`), called internally by the Spring Boot backend over REST. The frontend never calls the AI service directly — the backend proxies the call, applies authorisation, persists the result, and returns a shaped response.

## Consequences

- The core platform (FR-1 to FR-12) can be fully built, tested, and demoed without the AI service running at all — satisfying FR-15 structurally rather than through defensive coding alone.
- Two deployable units instead of one adds real operational surface (two Dockerfiles, two CI test jobs, two sets of environment variables) — accepted deliberately, because that surface is exactly what demonstrates microservice competence for the CV goal.
- An internal service-to-service auth mechanism (shared token) is required (see §10 of the Phase 1 document) — a small but real security decision the embedded option wouldn't have forced.
- If the AI feature were ever removed entirely, the core platform is unaffected — a natural consequence of the isolation this decision buys.
