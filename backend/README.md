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
├── social_service/       # /social (comments) + internal reactions
├── notification_service/ # /notifications + WebSocket
├── reading_service/      # /reading
├── ai_service/           # /ai
├── schema/               # ScyllaDB + PostgreSQL DDL
├── seed/                 # Dev seed script (opt-in via --profile dev)
├── docker-compose.yml    # Traefik + infra + all services
└── */Dockerfile          # per-service container images
```

## Quick start

```bash
cd backend
docker compose up --build
```

### Dev seed data (optional)

테스트 유저, 교회, 소그룹, 친구 관계, 묵상 노트를 자동으로 넣으려면 `dev` profile을 사용합니다:

```bash
docker compose --profile dev up --build
```

또는:

```bash
COMPOSE_PROFILES=dev docker compose up --build
```

기본 `docker compose up`만 실행하면 씨드는 **실행되지 않습니다**.

씨드 스크립트: [`seed/seed-dev-data.sh`](seed/seed-dev-data.sh) — 기존 HTTP API만 호출하며, `Grace Community Church`가 이미 있으면 스킵합니다.

| Email | Role |
|-------|------|
| `grace@biblelog.dev` | Grace Community Church 생성자 |
| `david@biblelog.dev` | 소그룹 리더 (Grace) |
| `sarah@biblelog.dev` | Grace 교회 · Youth Fellowship 멤버 |
| `mark@biblelog.dev` | Hope Chapel 생성자 |
| `demo@biblelog.app` | 기존 데모 계정 |

Dev login:

```bash
curl -X POST "http://localhost:8000/auth/dev/login?email=grace@biblelog.dev"
```

| Entry | URL |
|-------|-----|
| API (Traefik) | http://localhost:8000 |
| Traefik dashboard | http://localhost:8090 |

기본 dev login (씨드 없이도 사용 가능):

```bash
curl -X POST "http://localhost:8000/auth/dev/login?email=demo@biblelog.app"
```

## Routing (Traefik)

| Path prefix | Service | Internal port |
|-------------|---------|---------------|
| `/auth`, `/users`, `/friends`, `/follows`, `/churches`, `/small-groups` | user-service | 8001 |
| `/journal` | note-service | 8002 |
| `/social` | social-service | 8003 |
| `/feed` | feed-service | 8004 |
| `/reading` | reading-service | 8005 |
| `/ai` | ai-service | 8006 |
| `/notifications` | notification-service | 8007 |

WebSocket: `ws://localhost:8000/notifications/ws?token=<JWT>`

`social-service`는 public `/social/*`와 `/internal/*`를 모두 제공합니다.

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
| `social_service` | `/social`, `/internal/*` | ScyllaDB |
| `notification_service` | `/notifications`, WebSocket | PostgreSQL |
| `reading_service` | `/reading` | ScyllaDB |
| `ai_service` | `/ai` | PostgreSQL |

## AI provider

`AI_PROVIDER`: `mock` | `openai` | `anthropic`
