class AnalysisProviderError(Exception):
    """Raised when a provider cannot produce an analysis.

    Covers both configuration problems (e.g. a real provider selected
    without an API key) and runtime failures (network error, non-2xx
    response, a response that doesn't match the expected shape). The
    router turns this into a 502 so the caller can tell "the AI service is
    up but the model call failed" apart from "the AI service itself is
    unreachable" (FR-15's graceful degradation is the backend's problem to
    handle, not this service's to hide).
    """
