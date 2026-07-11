from __future__ import annotations

import asyncio
import json
import logging
from collections import defaultdict
from collections.abc import Awaitable, Callable
from typing import Any

from aiokafka import AIOKafkaConsumer, AIOKafkaProducer

from common.settings.base import BaseServiceSettings
from common.telemetry import traces_enabled

logger = logging.getLogger(__name__)

EventHandler = Callable[[dict[str, Any]], Awaitable[None] | None]

KafkaHeaders = list[tuple[str, bytes]] | None


def _inject_kafka_headers() -> KafkaHeaders:
    if not traces_enabled():
        return None

    from opentelemetry.propagate import inject

    carrier: dict[str, str] = {}
    inject(carrier)
    if not carrier:
        return None
    return [(key, value.encode("utf-8")) for key, value in carrier.items()]


def _extract_kafka_context(headers: KafkaHeaders):
    if not traces_enabled() or not headers:
        from opentelemetry import context

        return context.get_current()

    from opentelemetry import context
    from opentelemetry.propagate import extract

    carrier = {key: value.decode("utf-8") for key, value in headers}
    return extract(carrier)


class KafkaEventBus:
    def __init__(self, settings: BaseServiceSettings, *, group_id: str) -> None:
        self._settings = settings
        self._group_id = group_id
        self._producer: AIOKafkaProducer | None = None
        self._consumer: AIOKafkaConsumer | None = None
        self._handlers: dict[str, list[EventHandler]] = defaultdict(list)
        self._consumer_task: asyncio.Task[None] | None = None

    def subscribe(self, event_type: str, handler: EventHandler) -> None:
        self._handlers[event_type].append(handler)

    async def start(self) -> None:
        if not self._settings.kafka_enabled:
            logger.info("Kafka disabled — event bus will not start")
            return

        self._producer = AIOKafkaProducer(
            bootstrap_servers=self._settings.kafka_bootstrap_servers,
        )
        await self._producer.start()

        if self._handlers:
            self._consumer = AIOKafkaConsumer(
                self._settings.kafka_topic,
                bootstrap_servers=self._settings.kafka_bootstrap_servers,
                group_id=self._group_id,
                auto_offset_reset="latest",
            )
            await self._consumer.start()
            self._consumer_task = asyncio.create_task(self._consume_loop())

    async def stop(self) -> None:
        if self._consumer_task is not None:
            self._consumer_task.cancel()
            try:
                await self._consumer_task
            except asyncio.CancelledError:
                pass
            self._consumer_task = None

        if self._consumer is not None:
            await self._consumer.stop()
            self._consumer = None

        if self._producer is not None:
            await self._producer.stop()
            self._producer = None

    async def publish(self, event_type: str, payload: dict[str, Any]) -> None:
        if not self._settings.kafka_enabled:
            await self._dispatch_local(event_type, payload)
            return

        if self._producer is None:
            logger.warning("Kafka producer not started; dropping event %s", event_type)
            return

        message = json.dumps({"type": event_type, "payload": payload}).encode()

        if traces_enabled():
            from opentelemetry import trace
            from opentelemetry.trace import SpanKind

            tracer = trace.get_tracer(__name__)
            with tracer.start_as_current_span(
                "kafka.publish",
                kind=SpanKind.PRODUCER,
            ) as span:
                span.set_attribute("messaging.system", "kafka")
                span.set_attribute("messaging.destination.name", self._settings.kafka_topic)
                span.set_attribute("messaging.operation", "publish")
                span.set_attribute("messaging.message.type", event_type)
                headers = _inject_kafka_headers()
                await self._producer.send_and_wait(
                    self._settings.kafka_topic,
                    message,
                    headers=headers,
                )
            return

        await self._producer.send_and_wait(self._settings.kafka_topic, message)

    async def _consume_loop(self) -> None:
        assert self._consumer is not None
        async for record in self._consumer:
            try:
                data = json.loads(record.value.decode())
            except (json.JSONDecodeError, UnicodeDecodeError):
                logger.exception("Invalid Kafka event payload")
                continue
            event_type = data.get("type")
            payload = data.get("payload") or {}
            if not isinstance(event_type, str):
                continue

            parent_context = _extract_kafka_context(record.headers)
            if traces_enabled():
                from opentelemetry import trace
                from opentelemetry.trace import SpanKind

                tracer = trace.get_tracer(__name__)
                with tracer.start_as_current_span(
                    "kafka.consume",
                    context=parent_context,
                    kind=SpanKind.CONSUMER,
                ) as span:
                    span.set_attribute("messaging.system", "kafka")
                    span.set_attribute("messaging.destination.name", self._settings.kafka_topic)
                    span.set_attribute("messaging.operation", "process")
                    span.set_attribute("messaging.message.type", event_type)
                    span.set_attribute("messaging.kafka.consumer.group", self._group_id)
                    await self._dispatch_local(event_type, payload)
            else:
                await self._dispatch_local(event_type, payload)

    async def _dispatch_local(self, event_type: str, payload: dict[str, Any]) -> None:
        for handler in self._handlers.get(event_type, []):
            result = handler(payload)
            if asyncio.iscoroutine(result):
                await result


_producer_buses: dict[str, KafkaEventBus] = {}


def get_event_bus(settings: BaseServiceSettings, *, group_id: str) -> KafkaEventBus:
    key = group_id
    if key not in _producer_buses:
        _producer_buses[key] = KafkaEventBus(settings, group_id=group_id)
    return _producer_buses[key]


async def close_event_bus(group_id: str) -> None:
    bus = _producer_buses.pop(group_id, None)
    if bus is not None:
        await bus.stop()
