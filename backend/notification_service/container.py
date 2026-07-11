from __future__ import annotations

from dataclasses import dataclass

from common.clients.http import get_async_http_client
from common.clients.note import HttpNoteClient
from common.clients.relation import HttpRelationClient
from common.db.postgres import get_postgres
from common.events.kafka_bus import KafkaEventBus, get_event_bus
from notification_service.repositories.notification import NotificationRepository
from notification_service.repositories.notification_memory import MemoryNotificationRepository
from notification_service.repositories.notification_postgres import PostgresNotificationRepository
from notification_service.service import NotificationService
from notification_service.settings import NotificationServiceSettings, get_notification_settings
from notification_service.websocket_hub import WebSocketHub


@dataclass
class NotificationContainer:
    settings: NotificationServiceSettings
    notifications: NotificationRepository
    notification_service: NotificationService
    hub: WebSocketHub
    event_bus: KafkaEventBus


_container: NotificationContainer | None = None


def build_notification_container(
    settings: NotificationServiceSettings | None = None,
) -> NotificationContainer:
    settings = settings or get_notification_settings()
    postgres = get_postgres(settings)
    if postgres is not None:
        notifications: NotificationRepository = PostgresNotificationRepository(postgres)
    else:
        notifications = MemoryNotificationRepository()

    http_client = get_async_http_client()
    relations = HttpRelationClient(settings, http_client)
    notes = HttpNoteClient(settings, http_client)
    hub = WebSocketHub()
    event_bus = get_event_bus(settings, group_id="notification-service")
    notification_service = NotificationService(notifications, relations, notes, hub, event_bus)
    notification_service.register_event_handlers()
    return NotificationContainer(
        settings=settings,
        notifications=notifications,
        notification_service=notification_service,
        hub=hub,
        event_bus=event_bus,
    )


def get_notification_container() -> NotificationContainer:
    global _container
    if _container is None:
        _container = build_notification_container()
    return _container


def set_notification_container(container: NotificationContainer | None) -> None:
    global _container
    _container = container


def reset_notification_container() -> None:
    set_notification_container(None)
