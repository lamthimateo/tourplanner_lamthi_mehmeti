# Tour Planner — Backend

Spring Boot 3.1.4 REST API, Java 17, PostgreSQL 16 (via Docker Compose),
JWT auth, log4j2, iText 7 for PDF reports, Apache HttpClient for
OpenRouteService calls.

## Prerequisites

- **Java 17** — `run_backend.sh` (repo root) auto-selects it on macOS via
  `/usr/libexec/java_home -v 17` and on Linux by scanning
  `/usr/lib/jvm/java-17-*`. If you invoke `mvnw` directly, set
  `JAVA_HOME` to a JDK 17 yourself.
- **Docker** — for PostgreSQL (see `../docker-compose.yml`).

## 1. Start PostgreSQL

```bash
# From the repo root
docker compose up -d
```

PostgreSQL 16 runs on `localhost:5432`; user / password / db are all
`tourplanner`.

## 2. Configure environment

```bash
cp .env.example .env
# Edit .env and add your ORS_API_KEY (sign up free at https://openrouteservice.org/sign-up)
```

The file is gitignored so real secrets never reach the repository. Every
variable documented in `.env.example` has a safe default in
`src/main/resources/application.properties`.

## 3. Start the backend

```bash
# From the repo root (preferred — handles JDK selection + .env)
./run_backend.sh

# Or directly
JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./mvnw spring-boot:run
```

Backend: **http://localhost:8081**.

## API

All endpoints live under `/api`. Auth endpoints are open; everything else
requires a JWT in `Authorization: Bearer <token>`.

| Method | Path | Description |
|--------|------|-------------|
| POST   | `/api/auth/register` | Create an account, receive JWT |
| POST   | `/api/auth/login` | Log in, receive JWT |
| GET    | `/api/tours` | List tours for the current user |
| GET    | `/api/tours/{id}` | Get a single tour |
| POST   | `/api/tours` | Create a tour |
| PUT    | `/api/tours/{id}` | Update a tour |
| DELETE | `/api/tours/{id}` | Delete a tour |
| GET    | `/api/tours/search?q=...` | Full-text search tour IDs |
| GET    | `/api/tours/{id}/logs` | List logs for a tour |
| POST   | `/api/tours/{id}/logs` | Add a log |
| PUT    | `/api/tours/{id}/logs/{logId}` | Update a log |
| DELETE | `/api/tours/{id}/logs/{logId}` | Delete a log |
| POST   | `/api/tours/{id}/image` | Upload a tour image |
| GET    | `/api/tours/{id}/image` | Fetch a tour image |
| GET    | `/api/tours/{id}/export` | Export tour + logs as JSON |
| POST   | `/api/tours/import` | Import tour from JSON |
| GET    | `/api/tours/{id}/report` | Download per-tour PDF |
| GET    | `/api/reports/summary` | Download summary PDF |
| GET    | `/api/route?fromLat=…&fromLng=…&toLat=…&toLng=…` | Fetch ORS route GeoJSON |
| GET    | `/api/route/coordinates?location=…` | Geocode a location string |
| GET    | `/api/stats` | Aggregate statistics (unique feature) |

Every mutating operation is **user-scoped**: the service layer reads the
`userId` claim from the JWT via `AuthContext.getCurrentUserId()` and filters
every repository call accordingly. Attempts to touch another user's tour
come back as `404 Not Found` (not `403`, to avoid leaking that the tour
exists).

## Tests

```bash
cd backend && ./mvnw -o test
```

Expected: **56 tests, 0 failures**. Breakdown (§8 of the protocol):

- `TourServiceSearchTest`, `TourServiceCrudTest`, `TourLogServiceTest`,
  `TourDataTransferServiceTest`, `StatsServiceTest` — service-layer logic
- `AuthServiceTest`, `JwtUtilTest` — security + crypto
- `OpenRouteLocationServiceTest` — adapter delegation
- `ApiExceptionHandlerTest` — REST error mapping

## Configuration reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/tourplanner` | JDBC connection string |
| `DB_USERNAME` | `tourplanner` | Database user |
| `DB_PASSWORD` | `tourplanner` | Database password |
| `ORS_API_KEY` | *(empty)* | OpenRouteService API key |
| `JWT_SECRET` | built-in dev default | HMAC-SHA secret for JWT signing |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime |
| `SERVER_PORT` | `8081` | HTTP port |
