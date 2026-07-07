# BibleLog FastAPI Backend

OpenAPI contract: [`../openapi/biblelog-api.yaml`](../openapi/biblelog-api.yaml)

## Setup

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

## Run

```bash
fastapi dev
```

Production:

```bash
fastapi run
```

Alternatively, with uvicorn directly:

```bash
uvicorn app.main:app --reload --port 8000
```

- Swagger UI: http://localhost:8000/docs
- Health: http://localhost:8000/health

## OAuth

1. Google/Facebook OAuth 앱을 생성하고 `.env`에 client id/secret을 설정합니다.
2. Redirect URI: `http://localhost:8000/auth/{google|facebook}/callback`
3. 클라이언트는 `GET /auth/{provider}/authorize?redirect_uri=...`로 URL을 받아 브라우저에서 엽니다.

개발 중 OAuth 없이 테스트하려면:

```bash
curl -X POST "http://localhost:8000/auth/dev/login?email=demo@biblelog.app"
```

## AI Provider Switching

`AI_PROVIDER` 환경 변수만 변경하면 됩니다.

| Value | Description |
|-------|-------------|
| `mock` | 로컬 개발용 (기본) |
| `openai` | OpenAI Chat Completions |
| `anthropic` | Anthropic Messages API |

클라이언트는 `/ai/conversations/{id}/messages` 단일 인터페이스만 사용합니다.
