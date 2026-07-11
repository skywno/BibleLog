# AGENTS.md

## Cursor Cloud specific instructions

BibleLog is a single product with two halves:

- **Backend** (`backend/`): Python 3.12 FastAPI microservices (6 services) behind a Traefik
  gateway, plus ScyllaDB, PostgreSQL, Redis, Kafka, and OpenTelemetry/Jaeger. Run via Docker Compose.
- **Clients** (`androidApp/`, `desktopApp/`, `webApp/`, `iosApp/`, shared code in `shared/`):
  Kotlin/Compose Multiplatform. Built with the Gradle wrapper (`./gradlew`). All clients default to
  the gateway at `http://localhost:8000` (see `shared/.../data/remote/ApiConfig.*.kt`).

Standard run/test commands are documented in `README.md` (clients) and `backend/README.md` (backend);
the notes below only cover non-obvious cloud caveats.

### Docker (backend) — must start the daemon each session
- Docker Engine is installed in the VM image, but no init system runs here, so the daemon is **not**
  started automatically. Start it once per session: `sudo dockerd` (run it in a background/tmux
  session; it stays in the foreground otherwise).
- `/etc/docker/daemon.json` is preconfigured to use `fuse-overlayfs` and to disable the
  containerd-snapshotter (required for Docker to work in this VM). Do not remove that config.
- Bring up the whole backend: `cd backend && sudo docker compose up -d`. Add `--build` only after you
  change backend code/deps (deps live inside the images; a plain `up` will not pick up code changes).
- Gateway: `http://localhost:8000`, Traefik dashboard: `http://localhost:8090`. Ports 8001–8006 expose
  the individual services. Dev login (no OAuth needed):
  `curl -X POST "http://localhost:8000/auth/dev/login?email=demo@biblelog.app"`.
- Defaults need no secrets: `AI_PROVIDER=mock`, dev JWT secret, in-cluster datastores. OAuth and real
  AI keys are optional (`backend/.env.example`).
- The backend has **no test suite and no linters** configured (no pytest/ruff/black). Do not expect them.

### Clients (Gradle / Compose Multiplatform)
- JDK 21 and Node are already installed; the Gradle wrapper downloads Gradle 9.1.0 and all deps on demand.
- **Desktop app** is the easiest client to run headless-ish here. Run it against the VNC display:
  `DISPLAY=:1 ./gradlew :desktopApp:run`. Skiko logs `Cannot create Linux GL context` and falls back to
  software rendering — this is expected and non-fatal; the window still renders on display `:1`.
- The **Android SDK is not installed**, so `:androidApp` build/assemble tasks fail. Gradle *configuration*
  of the whole project (including `:androidApp`) still succeeds, so desktop/web/shared tasks work fine.
- iOS targets require macOS/Xcode and are unavailable on this Linux VM (the `iosSimulatorArm64Test`
  disabled-target warning is expected).
- Tests: `./gradlew :shared:jvmTest` runs. Web tests: `:shared:jsTest` / `:shared:wasmJsTest`.
- CORS on the backend is `*`, so the web client (`./gradlew :webApp:jsBrowserDevelopmentRun`) can also
  reach the gateway if you prefer a browser-based client.
