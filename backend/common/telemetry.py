from __future__ import annotations

import json
import logging
import os
from datetime import UTC, datetime
from typing import Any

from fastapi import FastAPI

from common.config import get_settings

logger = logging.getLogger(__name__)

_instrumented = False


def traces_enabled() -> bool:
    return os.getenv("OTEL_TRACES_ENABLED", "false").lower() in ("true", "1", "yes")


def _deployment_environment() -> str:
    if os.getenv("DEPLOYMENT_ENVIRONMENT"):
        return os.getenv("DEPLOYMENT_ENVIRONMENT", "development")
    return "development" if get_settings().debug else "production"


def setup_telemetry(service_name: str, *, version: str = "0.4.0") -> None:
    setup_structured_logging()

    if not traces_enabled():
        logger.debug("OpenTelemetry tracing disabled (OTEL_TRACES_ENABLED=false)")
        return

    from opentelemetry import trace
    from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
    from opentelemetry.sdk.resources import DEPLOYMENT_ENVIRONMENT, SERVICE_NAME, SERVICE_VERSION, Resource
    from opentelemetry.sdk.trace import TracerProvider
    from opentelemetry.sdk.trace.export import BatchSpanProcessor

    endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317")
    resource = Resource.create(
        {
            SERVICE_NAME: os.getenv("OTEL_SERVICE_NAME", service_name),
            SERVICE_VERSION: version,
            DEPLOYMENT_ENVIRONMENT: _deployment_environment(),
        }
    )
    provider = TracerProvider(resource=resource)
    provider.add_span_processor(BatchSpanProcessor(OTLPSpanExporter(endpoint=endpoint, insecure=True)))
    trace.set_tracer_provider(provider)

    _instrument_runtime_libraries()
    logger.info("OpenTelemetry tracing enabled for %s -> %s", service_name, endpoint)


def _instrument_runtime_libraries() -> None:
    global _instrumented
    if _instrumented:
        return

    from opentelemetry.instrumentation.httpx import HTTPXClientInstrumentor
    from opentelemetry.instrumentation.logging import LoggingInstrumentor
    from opentelemetry.instrumentation.psycopg import PsycopgInstrumentor
    from opentelemetry.instrumentation.redis import RedisInstrumentor

    HTTPXClientInstrumentor().instrument()
    PsycopgInstrumentor().instrument()
    RedisInstrumentor().instrument()
    LoggingInstrumentor().instrument(set_logging_format=False)
    _instrumented = True


def _server_request_hook(span: Any, scope: dict[str, Any]) -> None:
    if span is None or not span.is_recording():
        return
    raw_headers = scope.get("headers") or []
    headers = {key.decode("latin-1").lower(): value.decode("latin-1") for key, value in raw_headers}
    user_id = _try_decode_user_id(headers.get("authorization"))
    if user_id:
        span.set_attribute("enduser.id", user_id)


def instrument_app(app: FastAPI) -> None:
    if not traces_enabled():
        return

    from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor

    excluded = os.getenv("OTEL_PYTHON_FASTAPI_EXCLUDED_URLS", "/health,health")
    FastAPIInstrumentor.instrument_app(
        app,
        excluded_urls=excluded,
        server_request_hook=_server_request_hook,
    )


def _try_decode_user_id(authorization: str | None) -> str | None:
    if not authorization:
        return None
    scheme, _, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or not token:
        return None
    try:
        from jose import JWTError, jwt

        settings = get_settings()
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
    except JWTError:
        return None
    if payload.get("type") != "access":
        return None
    user_id = payload.get("sub")
    return str(user_id) if user_id else None


class JsonLogFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, Any] = {
            "timestamp": datetime.fromtimestamp(record.created, tz=UTC).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)

        otel_trace_id = getattr(record, "otelTraceID", None)
        otel_span_id = getattr(record, "otelSpanID", None)
        if otel_trace_id and otel_trace_id != "0" * 32:
            payload["trace_id"] = otel_trace_id
        if otel_span_id and otel_span_id != "0" * 16:
            payload["span_id"] = otel_span_id

        for key, value in record.__dict__.items():
            if key.startswith("_") or key in {
                "name",
                "msg",
                "args",
                "created",
                "filename",
                "funcName",
                "levelname",
                "levelno",
                "lineno",
                "module",
                "msecs",
                "message",
                "pathname",
                "process",
                "processName",
                "relativeCreated",
                "stack_info",
                "exc_info",
                "exc_text",
                "thread",
                "threadName",
                "taskName",
                "otelTraceID",
                "otelSpanID",
                "otelTraceSampled",
                "otelServiceName",
            }:
                continue
            payload[key] = value

        return json.dumps(payload, default=str)


def setup_structured_logging() -> None:
    if os.getenv("STRUCTURED_LOGGING", "true").lower() not in ("true", "1", "yes"):
        return

    root = logging.getLogger()
    if any(isinstance(handler.formatter, JsonLogFormatter) for handler in root.handlers):
        return

    handler = logging.StreamHandler()
    handler.setFormatter(JsonLogFormatter())
    root.handlers = [handler]
    root.setLevel(logging.INFO if not get_settings().debug else logging.DEBUG)


def record_exception_on_span(exc: BaseException) -> None:
    if not traces_enabled():
        return

    from opentelemetry import trace

    span = trace.get_current_span()
    if span.is_recording():
        span.record_exception(exc)
        span.set_attribute("error.type", type(exc).__name__)
