# BibleLog FastAPI Backend (Microservices Monorepo)

OpenAPI: [`../openapi/biblelog-api.yaml`](../openapi/biblelog-api.yaml)  
Architecture: [`../architecture.md`](../architecture.md)

## Layout

```
backend/
├── traefik/              # API Gateway (Traefik dynamic routing)
├── common/               # Config, models, DB clients, Kafka events, JWT, contracts
├── user_service/         # /auth, /users (+ internal relation APIs)
├── note_service/         # /journal (+ internal note APIs)
├── feed_service/         # /feed (Redis ZSET + assembly)
├── social_service/       # internal reactions/comments APIs
├── reading_service/      # /reading
├── ai_service/           # /ai
├── schema/               # ScyllaDB + PostgreSQL DDL
├── docker-compose.yml    # Traefik + infra + all services
└── */Dockerfile          # per-service container images
```

## Quick start

```bash
cd backend
docker compose up --build
```

| Entry | URL |
|-------|-----|
| API (Traefik) | http://localhost:8000 |
| Traefik dashboard | http://localhost:8090 |

Dev login:

```bash
curl -X POST "http://localhost:8000/auth/dev/login?email=demo@biblelog.app"
```

## Routing (Traefik)

| Path prefix | Service | Internal port |
|-------------|---------|---------------|
| `/auth`, `/users` | user-service | 8001 |
| `/journal` | note-service | 8002 |
| `/feed` | feed-service | 8004 |
| `/reading` | reading-service | 8005 |
| `/ai` | ai-service | 8006 |

`social-service`는 `/internal/*`만 노출하며 Traefik public 라우트에 포함되지 않습니다.

라우팅 설정: [`traefik/dynamic/routes.yml`](traefik/dynamic/routes.yml)

## Direct service access (debug)

각 서비스 포트가 호스트에도 노출됩니다 (8001–8006). Swagger는 서비스별로 확인할 수 있습니다.

## Feed API

`GET /feed` → `FeedPageResponse` (`items`, `next_cursor`, `has_more`)

피드 타임라인은 DB에 저장하지 않습니다. Redis Sorted Set에 `note_id`만 캐시합니다.

## Per-service packages

| Package | Router prefix | Storage |
|---------|---------------|---------|
| `user_service` | `/auth`, `/users` | PostgreSQL |
| `note_service` | `/journal` | ScyllaDB |
| `feed_service` | `/feed` | Redis |
| `social_service` | `/internal/*` | ScyllaDB |
| `reading_service` | `/reading` | ScyllaDB |
| `ai_service` | `/ai` | PostgreSQL |

## AI provider

`AI_PROVIDER`: `mock` | `openai` | `anthropic`
