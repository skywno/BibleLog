from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass
class AiCompletionResult:
    content: str
    suggested_book_id: int | None = None
    suggested_chapter: int | None = None
    suggested_verse: int | None = None


class AiProvider(ABC):
    name: str

    @abstractmethod
    async def complete(self, user_message: str, *, mode: str = "chat") -> AiCompletionResult:
        ...


class MockAiProvider(AiProvider):
    name = "mock"

    async def complete(self, user_message: str, *, mode: str = "chat") -> AiCompletionResult:
        prefix = "함께 기도드릴게요. " if mode == "prayer" else ""
        if any(word in user_message for word in ("슬프", "힘들")):
            return AiCompletionResult(
                content=(
                    f"{prefix}마음이 무거우시군요. 시편 34:18처럼, "
                    "주님은 상한 마음을 가까이 하십니다. 지금 느끼시는 감정을 "
                    "있 그대로 가져가셔도 괜찮아요."
                ),
                suggested_book_id=19,
                suggested_chapter=34,
                suggested_verse=18,
            )
        return AiCompletionResult(
            content=(
                f"{prefix}말씀 나눠 주셔서 감사합니다. "
                "오늘 묵상하신 내용 속에서도 주님께서 함께하고 계십니다."
            ),
            suggested_book_id=43,
            suggested_chapter=3,
            suggested_verse=16,
        )


class OpenAiProvider(AiProvider):
    name = "openai"

    def __init__(self, api_key: str, model: str) -> None:
        self._api_key = api_key
        self._model = model

    async def complete(self, user_message: str, *, mode: str = "chat") -> AiCompletionResult:
        import httpx

        system_prompt = (
            "You are a gentle Christian spiritual companion. "
            "Respond in Korean with empathy and Scripture references. "
            "Never preach or condemn."
        )
        if mode == "prayer":
            system_prompt += " Lead a short, warm prayer together with the user."

        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(
                "https://api.openai.com/v1/chat/completions",
                headers={"Authorization": f"Bearer {self._api_key}"},
                json={
                    "model": self._model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_message},
                    ],
                },
            )
            response.raise_for_status()
            content = response.json()["choices"][0]["message"]["content"]
        return AiCompletionResult(content=content)


class AnthropicAiProvider(AiProvider):
    name = "anthropic"

    def __init__(self, api_key: str, model: str) -> None:
        self._api_key = api_key
        self._model = model

    async def complete(self, user_message: str, *, mode: str = "chat") -> AiCompletionResult:
        import httpx

        system_prompt = (
            "You are a gentle Christian spiritual companion. "
            "Respond in Korean with empathy and Scripture references. "
            "Never preach or condemn."
        )
        if mode == "prayer":
            system_prompt += " Lead a short, warm prayer together with the user."

        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(
                "https://api.anthropic.com/v1/messages",
                headers={
                    "x-api-key": self._api_key,
                    "anthropic-version": "2023-06-01",
                    "content-type": "application/json",
                },
                json={
                    "model": self._model,
                    "max_tokens": 1024,
                    "system": system_prompt,
                    "messages": [{"role": "user", "content": user_message}],
                },
            )
            response.raise_for_status()
            content = response.json()["content"][0]["text"]
        return AiCompletionResult(content=content)


from shared.config import Settings


def create_ai_provider(settings: Settings) -> AiProvider:
    provider = settings.ai_provider.lower()
    if provider == "openai":
        if not settings.openai_api_key:
            raise RuntimeError("OPENAI_API_KEY is required when AI_PROVIDER=openai")
        return OpenAiProvider(settings.openai_api_key, settings.openai_model)
    if provider == "anthropic":
        if not settings.anthropic_api_key:
            raise RuntimeError("ANTHROPIC_API_KEY is required when AI_PROVIDER=anthropic")
        return AnthropicAiProvider(settings.anthropic_api_key, settings.anthropic_model)
    return MockAiProvider()
