# BibleLog Multi-User Simulation Framework

The `:simulation` JVM module provides reusable load and integration testing for the BibleLog backend without manually opening multiple app instances.

## Prerequisites

1. Start the backend: `cd backend && docker compose up --build`
2. Ensure `DEBUG=true` so `POST /auth/dev/login` is available

## Running

```bash
# Fast CI smoke test (10 users, 30 seconds)
./gradlew :simulation:test

# Full load test (100 users, 5 minutes) — requires env flag
BIBLELOG_LOAD_TEST=true ./gradlew :simulation:loadTest
```

## Architecture

```
SimulationRunner
  └── UserScenario (per user, seeded Random)
        └── TestUser
              ├── SimulatedApiClient → BibleLogApiClient
              ├── SimulatedWebSocketClient → BibleLogWebSocketClient
              └── EventRecorder
```

## Configuration (`SimulationConfig`)

| Field | Default | Description |
|-------|---------|-------------|
| `userCount` | 10 | Concurrent simulated users |
| `duration` | 30s | Per-user scenario runtime |
| `seed` | 42 | Reproducible `Random` seed |
| `actionWeights` | see below | Weighted action probabilities |
| `webSocketEnabled` | true | Connect each user to `/notifications/ws` |
| `network` | `NetworkConfig` | Latency, packet loss, retries, WS faults |

### Default action weights (example scenario)

| Action | Weight | Maps to |
|--------|--------|---------|
| createPost | 10% | `POST /journal/notes` |
| like | 35% | `POST /feed/{id}/reactions` (faith reaction) |
| comment | 20% | `POST /social/notes/{id}/comments` |
| follow | 10% | `POST /follows/{userId}` |
| refreshFeed | 25% | `GET /feed` |

## Terminology

| Generic term | BibleLog API |
|--------------|--------------|
| post | meditation note (`/journal/notes`) |
| like | faith reaction (`empathy`, etc.) |
| follow | asymmetric follow (`POST /follows/{id}`) |
| befriend | friend request flow (`POST /friends/requests`) |

## Assertions

After simulation completes, `AssertionSuite` verifies:

- Feed consistency across users (public feed, latest sort)
- No duplicate note IDs in a feed page
- WebSocket events received when enabled
- Eventual consistency within `quiescenceTimeout` (default 30s)

## Report

`SimulationReport` includes total requests, success/failure rate, average and P95 latency, WebSocket reconnect count, action breakdown, and consistency violations. Use `ReportFormatter.format(report)` for human-readable output.

## Extending actions

Implement `UserAction` and register via `ActionRegistry`:

```kotlin
class JoinChurchAction : UserAction {
    override val name = "joinChurch"
    override suspend fun execute(user: TestUser, context: ActionContext) = ...
}
```

## Design decisions

- **Structured concurrency**: `coroutineScope` + per-user `async` jobs; no `GlobalScope`
- **Immutable config/models**: reproducible runs from saved `SimulationConfig`
- **Separate layers**: API (`SimulatedApiClient`), scenario (`UserScenario`), assertions, reporting
