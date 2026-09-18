"""Reads the X-Request-Id header the backend's RequestCorrelationFilter forwards
(see docs/architecture.md's Phase 14 section) into a contextvar the JSON log
formatter picks up, and echoes it back on the response - the FastAPI side of the
same request-tracing scheme the backend uses.
"""

from collections.abc import Awaitable, Callable

from fastapi import Request, Response

from app.logging_config import request_id_var

REQUEST_ID_HEADER = "X-Request-Id"


async def request_correlation_middleware(
    request: Request, call_next: Callable[[Request], Awaitable[Response]]
) -> Response:
    request_id = request.headers.get(REQUEST_ID_HEADER)
    token = request_id_var.set(request_id)
    try:
        response = await call_next(request)
    finally:
        request_id_var.reset(token)
    if request_id is not None:
        response.headers[REQUEST_ID_HEADER] = request_id
    return response
