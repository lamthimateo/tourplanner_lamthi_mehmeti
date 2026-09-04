# Tour Planner

> SWEN 2 — Software Engineering 2 final project.
> Angular 17 frontend + Spring Boot 3 backend + PostgreSQL + OpenRouteService + Leaflet.

**Git repository:** https://github.com/lamthimateo/tourplanner_lamthi_mehmeti

Users register, plan tours, compute routes, attach logs of actual outings,
search across everything (including computed attributes), export tours to
JSON, generate PDF reports, and view a per-transport-type statistics
dashboard.

---

## Repository layout

```
tourplanner_lamthi_mehmeti/
├── backend/             Spring Boot 3 REST API (Java 17)
├── frontend/            Angular 17 standalone app
├── docs/                project protocol (PDF for Moodle hand-in)
│   └── diagrams/        PlantUML (architecture, UML, sequences, use cases)
├── docker-compose.yml   PostgreSQL 16 for local dev
├── run_backend.sh       start the backend (auto-detects JDK 17)
├── run_frontend.sh      start Angular dev server (Node 22 via nvm)
└── run_project.sh       Postgres + backend + frontend in one go
```

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java | 17 | `run_backend.sh` auto-selects it on macOS via `/usr/libexec/java_home -v 17` |
| Maven | bundled | `mvnw` is checked in |
| Node | 22 | `run_frontend.sh` loads it via `nvm use 22` |
| Docker | any recent | For `docker compose up -d` |

**Platform notes:** `run_frontend.sh` assumes Homebrew's nvm install path.
Linux users, or macOS users with a curl-installed nvm, should run `nvm use 22`
manually before `./run_frontend.sh`. Windows users should use WSL, or run the
commands inside `run_backend.sh`/`run_frontend.sh` directly.

---

## Quick start

```bash
# 1. Configuration (first time only)
cp backend/.env.example backend/.env
# then edit backend/.env and set ORS_API_KEY to your OpenRouteService key

# 2. Start everything
./run_project.sh
```

This brings up Postgres, the backend, and the frontend. Then open:

- UI:        http://localhost:4200
- API root:  http://localhost:8081/api

### Run the pieces individually

```bash
docker compose up -d      # PostgreSQL only
./run_backend.sh          # backend only (http://localhost:8081)
./run_frontend.sh         # frontend only (http://localhost:4200)
```

---

## First login

There's no seeded account. On the login screen, switch to the register form
to create one (calls `POST /api/auth/register`) — username 3-50 chars,
password 4-100 chars, no email verification. Then log in with those
credentials.

---

## Tests

```bash
cd backend
export JAVA_HOME="$(/usr/libexec/java_home -v 17)"   # macOS; tests need JDK 17
./mvnw test
```

Expected output: **Tests run: 56, Failures: 0, Errors: 0** (55 unit tests + 1 H2 integration test).

**Important:** use **Java 17** for `./mvnw test`. On newer JDKs (e.g. 23) Mockito/ByteBuddy may fail; `run_backend.sh` auto-selects JDK 17 on macOS.

The test suite covers the service layer, JWT utilities, REST error mapping,
the location adapter, and one Spring Boot + H2 integration test for tour
delete with logs. See the project protocol (§8) for the rationale.

---

## Configuration

All runtime configuration lives in `backend/.env` (gitignored). The committed
[`backend/.env.example`](backend/.env.example) documents every variable with
safe defaults. Nothing sensitive is stored in `application.properties`; every
value is read via `${NAME:default}` placeholders.

Key variables:

| Variable | Default | Purpose |
|---|---|---|
| `ORS_API_KEY` | *(empty)* | OpenRouteService key for geocoding + routing |
| `DB_URL` | `jdbc:postgresql://localhost:5432/tourplanner` | JDBC URL |
| `DB_USERNAME` | `tourplanner` | DB user |
| `DB_PASSWORD` | `tourplanner` | DB password |
| `JWT_SECRET` | built-in dev default | HMAC key for JWT signing |
| `JWT_EXPIRATION_MS` | `86400000` | 24 h token lifetime |
| `SERVER_PORT` | `8081` | Backend port |
| `APP_BASE_DIR` | `~/TourPlanner` | Root folder for images, reports, logs |

The frontend's API base URL lives in
[`frontend/src/environments/environment.ts`](frontend/src/environments/environment.ts).

---

## Documentation

The complete project protocol (architecture, UML diagrams, wireframes, time
tracking, testing strategy, sprint log) is in
[`docs/protocol.pdf`](docs/protocol.pdf). Export this PDF separately for the
Moodle hand-in if required.
