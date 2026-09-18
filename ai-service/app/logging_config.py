"""Structured (JSON) logging, matching the backend's own switch to JSON console
logs (Phase 14 observability) - one log event per line, machine-parseable by
CloudWatch Logs or any other aggregator, with the request ID the backend
forwards (see ``app/middleware.py`` and
``com.incidentplatform.observability.RequestCorrelationFilter``) included so one
request's logs can be correlated across both services.

A small hand-rolled formatter rather than a third-party JSON-logging package -
this project's exact field set is a handful of lines, and formatting for
CloudWatch/`jq` doesn't need a dependency to get.
"""

import contextvars
import json
import logging
from datetime import UTC, datetime

request_id_var: contextvars.ContextVar[str | None] = contextvars.ContextVar(
    "request_id", default=None
)


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "@timestamp": datetime.fromtimestamp(record.created, tz=UTC).isoformat(),
            "level": record.levelname,
            "logger_name": record.name,
            "message": record.getMessage(),
            "service": "incident-platform-ai-service",
        }
        request_id = request_id_var.get()
        if request_id is not None:
            payload["requestId"] = request_id
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload)


def configure_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter())
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(logging.INFO)

    # uvicorn attaches its own handlers directly to "uvicorn"/"uvicorn.access"/
    # "uvicorn.error" (its default logging config runs before this app module
    # is even imported), so without this, access/startup logs would keep
    # printing in uvicorn's own plain format instead of JSON. Clearing their
    # handlers lets the records propagate up to root's JSON handler instead.
    for name in ("uvicorn", "uvicorn.error", "uvicorn.access"):
        uvicorn_logger = logging.getLogger(name)
        uvicorn_logger.handlers = []
        uvicorn_logger.propagate = True
