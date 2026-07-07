from fastapi import APIRouter, Depends, HTTPException, status

from app.ai.providers import create_ai_provider
from app.deps import CurrentUserIdDep, SettingsDep, get_current_user_id
from app.models import (
    AiConversationSummary,
    AiMessage,
    BibleReference,
    SendAiMessageRequest,
    SendAiMessageResponse,
)
from app.store import store

router = APIRouter(
    prefix="/ai",
    tags=["ai"],
    dependencies=[Depends(get_current_user_id)],
)


@router.get("/conversations")
def list_ai_conversations(user_id: CurrentUserIdDep) -> list[AiConversationSummary]:
    conversations = store.ai_conversations.get(user_id, [])
    return [AiConversationSummary(**conversation) for conversation in conversations]


@router.post("/conversations", status_code=status.HTTP_201_CREATED)
def create_ai_conversation(user_id: CurrentUserIdDep) -> AiConversationSummary:
    conversation = store.create_ai_conversation(user_id)
    return AiConversationSummary(**conversation)


@router.get("/conversations/{conversation_id}/messages")
def list_ai_messages(
    conversation_id: str,
    user_id: CurrentUserIdDep,
) -> list[AiMessage]:
    return [AiMessage(**message) for message in store.list_ai_messages(conversation_id)]


@router.post("/conversations/{conversation_id}/messages")
async def send_ai_message(
    conversation_id: str,
    body: SendAiMessageRequest,
    user_id: CurrentUserIdDep,
    settings: SettingsDep,
) -> SendAiMessageResponse:
    if conversation_id not in store.ai_messages:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Conversation not found")

    provider = create_ai_provider(settings)
    result = await provider.complete(body.content, mode=body.mode)

    suggested = None
    if result.suggested_book_id:
        suggested = BibleReference(
            book_id=result.suggested_book_id,
            start_chapter=result.suggested_chapter or 1,
            start_verse=result.suggested_verse or 1,
            end_chapter=result.suggested_chapter or 1,
            end_verse=result.suggested_verse or 1,
        )

    return store.append_ai_messages(
        conversation_id,
        body.content,
        result.content,
        suggested,
        provider.name,
    )
