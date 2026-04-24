# Tour Planner

> SWEN 2 — Software Engineering 2 final project.
> Angular 17 frontend + Spring Boot 3 backend + PostgreSQL + OpenRouteService + Leaflet.

Users register, plan tours, compute routes, attach logs of actual outings,
search across everything (including computed attributes), export tours to
JSON, generate PDF reports, and view a per-transport-type statistics
dashboard.

---

## Repository layout

```
one_repo/
├── backend/             Spring Boot 3 REST API (Java 17)
├── frontend/            Angular 17 standalone app
├── docs/                protocol, UML, wireframes, time tracking
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

## Tests

```bash
cd backend && ./mvnw -o test
```

Expected output: **Tests run: 55, Failures: 0, Errors: 0**.

The test suite covers the service layer, JWT utilities, REST error mapping,
and the location adapter (`OpenRouteLocationService`). See
[`docs/protocol.md`](docs/protocol.md) §8 for the rationale.

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

---

## Documentation

The full protocol, UML diagrams, wireframes, and time tracking live in
[`docs/`](docs). Start with [`docs/protocol.md`](docs/protocol.md) — it
covers architecture, MVVM, patterns, the unique feature, testing strategy,
and the sprint log.
