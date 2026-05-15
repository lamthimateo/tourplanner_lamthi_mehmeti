# Tour Planner — Project Protocol

**Course:** SWEN 2 — Software  Engineering 2
**Team:** Lamthi, Mehmeti
**Semester:** SS 2026
**Repository:** one-repo (monorepo: Angular frontend + Spring Boot backend)
**Git:** _add your final GitHub URL here before handing in_

---

## Table of Contents

1. Project Overview
2. Technology Stack
3. Architecture
4. MVVM on the Frontend
5. Design Patterns
6. Unique Feature — Statistics Dashboard
7. Library Decisions & Lessons Learned
8. Unit Testing Strategy
9. Known Design Constraints
10. Sprint Log
11. How to Run

---

## 1. Project Overview

Tour Planner is a web application for planning and journaling tours.
A user registers an account, creates tours (bike, hike, run, or drive), and
attaches tour logs that record each individual outing. The app calls
OpenRouteService to compute distance / duration / geometry, draws the route on
a Leaflet map, produces PDF reports, and shows aggregate statistics for the
user's tours.

### Primary use cases

- Register / login (JWT); data is private per user.
- Create, update, delete tours; upload a tour image.
- Compute a route (distance + estimated time + map polyline).
- Create, update, delete tour logs (CRUD, validated).
- Full-text search across tours, logs, and **computed** attributes.
- Import / export a tour (+ its logs) as JSON.
- Generate a per-tour PDF report and a global summary PDF.
- View per-transport-type statistics (unique feature).

---

## 2. Technology Stack

| Layer | Tech |
|---|---|
| Frontend | Angular 17 (standalone), FormsModule, Leaflet |
| Backend | Spring Boot 3.1.4 (web, data-jpa, security, validation, log4j2) |
| Auth | Spring Security + JJWT 0.11.5 + BCrypt |
| Persistence | Hibernate/JPA → PostgreSQL 16 (Docker Compose) |
| External | OpenRouteService (routing + geocoding), Nominatim (autocomplete) |
| PDF | iText 7 Community |
| HTTP | Apache HttpClient (backend → ORS) |
| Testing | JUnit 5, Mockito |

Configuration lives in `backend/.env` (gitignored) and
`backend/src/main/resources/application.properties` (defaults + `${ENV}`
placeholders). No credentials or API keys are committed.

---

## 3. Architecture

The application is a classic three-layer web architecture:

```
┌──────────────────────────────────────────────────────────────┐
│ Presentation (Angular)                                       │
│   View (AppComponent template)                               │
│   ViewModel (TourViewModel, TourLogViewModel)                │
│   Model binding via [(ngModel)]                              │
└──────────────────────────────────────────────────────────────┘
                       │  HTTPS + JWT
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ Presentation (Spring MVC)                                    │
│   *Controllers (TourController, AuthController, …)           │
│   ApiExceptionHandler (@RestControllerAdvice)                │
└──────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ Business Layer                                               │
│   TourService, TourLogService, AuthService                   │
│   StatsService, ReportService, TourDataTransferService       │
│   LocationService (interface) → OpenRouteLocationService     │
└──────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ Data Access Layer                                            │
│   Spring Data JPA Repositories                               │
│   TourRepository, TourLogRepository, UserRepository          │
└──────────────────────────────────────────────────────────────┘
                       │
                       ▼
                    PostgreSQL
```

Each layer only talks to the layer directly below it. Layers define their own
exceptions (`TourNotFoundException`, `TourLogNotFoundException`,
`IllegalArgumentException` for validation), which the presentation layer
translates to HTTP status codes in `ApiExceptionHandler`.

Full class-level diagrams live in `docs/architecture.puml`,
`docs/uml.puml`, `docs/sequence-diagrams.puml`, and `docs/usecases.puml`.

---

## 4. MVVM on the Frontend

| Role | Implementation |
|---|---|
| View | `AppComponent` template (Angular bindings only, no imperative DOM code except the Leaflet map) |
| ViewModel | `TourViewModel`, `TourLogViewModel` — mutable form state, `isValid()` validation, `toTour()` / `toLog()` mapping to Model objects |
| Model | `Tour`, `TourLog` interfaces + `ApiService`, `AuthService` (REST + JWT) |

Two-way data binding (`[(ngModel)]`) is the MVVM glue: the template reads from
and writes to ViewModel properties automatically. Saving a tour flows as:

1. Edits → `[(ngModel)]` writes into `TourViewModel`.
2. `Save` → component calls `selectedTour.isValid()` (validation lives in the VM).
3. If valid → `selectedTour.toTour()` projects the VM to a `Tour` model.
4. `ApiService.createTour/updateTour(...)` sends the model to the backend.
5. Backend response → `TourViewModel.from(saved)` re-wraps it for the template.

---

## 5. Design Patterns

The project uses several patterns deliberately; the ones listed here are the
ones we would highlight in a review.

### 5.1 Repository Pattern
`TourRepository`, `TourLogRepository`, `UserRepository` are Spring Data JPA
interfaces. The service layer calls methods like `findByUserId(...)` without
knowing anything about SQL or the Hibernate dialect.

### 5.2 Adapter Pattern
`LocationService` is a business-layer interface. `OpenRouteLocationService`
adapts the low-level `OpenRouteService` HTTP client so that controllers can
swap the provider later without changing their call sites.

### 5.3 MVVM (Frontend)
See section 4. The ViewModel classes own the validation and conversion logic,
not the component, which keeps the View purely declarative.

### 5.4 Observer / Reactive (Frontend)
`ApiService` methods return cold RxJS `Observable<T>`s. Components subscribe
and react to `next` / `error`. This is the standard Angular reactive flow.

### 5.5 Singleton Services (Frontend)
`@Injectable({providedIn: 'root'})` guarantees that `ApiService` and
`AuthService` are single app-scoped instances.

### 5.6 Filter / Chain of Responsibility (Backend)
`JwtAuthFilter extends OncePerRequestFilter`. Requests go through the security
filter chain; the JWT filter pulls the Bearer token from the header, decodes
it, and populates the `SecurityContext`.

### 5.7 DTO
`TourResponseDto`, `TourExportDto`, `StatsDto`, `AuthRequest`, `AuthResponse`
decouple wire-format from JPA entities. `TourResponseDto` is where the
computed attributes (`popularity`, `childFriendliness`) live — the entity
never holds them.

---

## 6. Unique Feature — Statistics Dashboard

Endpoint: `GET /api/stats` → `StatsDto`.

`StatsService.getStats()` aggregates the current user's data:

- `totalTours`, `totalLogs`, `totalDistanceKm`, `totalTimeHours`, `avgRating`
- `byTransportType[]`: per-transport breakdown (tour count, log count, avg
  distance per log, avg rating) sorted by tour count descending.

All aggregation is in Java streams — no native SQL, no duplication of the
derivation logic across layers. The result is displayed in a collapsible
panel in the sidebar; each metric is rendered by the reusable
`<app-stat-card>` component.

**Why useful:** the user gets a snapshot of how much they've toured, which
transport modes they prefer, and how their ratings trend, without opening
individual tour records.

---

## 7. Library Decisions & Lessons Learned

### Backend

| Library | Version | Why |
|---|---|---|
| Spring Boot | 3.1.4 | Industry standard; auto-config; Spring Data JPA eliminates hand-written SQL |
| Spring Security + BCrypt | via Spring Boot | BCrypt is the modern default for password hashing (adaptive cost, built-in salt) |
| JJWT | 0.11.5 | Cleaner 0.11.x API (`Keys.hmacShaKeyFor`, `Jwts.parserBuilder()`) |
| iText 7 | 7.2.5 | PDF tables, vector drawing, widely documented |
| Apache HttpClient | 4.5.x | `RestTemplate` was removed; `CloseableHttpClient` + Jackson is reliable |
| PostgreSQL JDBC | 42.5.4 | Matches the 16-alpine image used in `docker-compose.yml` |
| spring-dotenv | 4.0.0 | Loads `backend/.env` into `@Value` placeholders automatically |

### Frontend

| Library | Why |
|---|---|
| Angular 17 standalone | No NgModules; less boilerplate |
| Leaflet | Lightweight; works with free OSM tiles (no map-tile API key) |
| RxJS | Observable-based async composition |
| zone.js | Required polyfill for Angular change detection |

### Lessons Learned

1. **Field-name mismatches break silently.** Jackson silently drops unknown
   fields, so a mismatch (`fromLocation` vs `origin`) looks like "data not
   saving" with no error. Verifying the JSON contract end-to-end saved us
   hours.
2. **Schema drift on rename.** Renaming a column on an existing JPA entity
   corrupts the existing DB file. In dev we run `spring.jpa.hibernate.ddl-auto=update`;
   occasionally we have to drop tables manually when a rename can't be
   inferred.
3. **JWT credentials vs principal.** Spring Security's
   `UsernamePasswordAuthenticationToken` has a `principal` (username) and
   arbitrary `credentials`. We store the `userId` in credentials so that
   `AuthContext.getCurrentUserId()` is a one-liner.
4. **Angular template syntax.** `(click)="if(x) foo()"` does not parse —
   Angular templates forbid statement-level keywords. Extract to a method.
5. **Ownership is a service concern, not a controller concern.** We moved
   every ownership check into `TourService` / `TourLogService` so no
   controller can forget the user filter.

---

## 8. Unit Testing Strategy

All tests are **unit tests** using **JUnit 5** and **Mockito** — no Spring
context, so each test finishes in milliseconds.

| Test class | Focus | # |
|---|---|---|
| `TourServiceSearchTest` | Full-text search across tour/log/computed fields | 6 |
| `TourServiceCrudTest` | CRUD + per-user isolation + derived metrics | 9 |
| `TourLogServiceTest` | Log CRUD + parent-tour ownership enforcement | 10 |
| `AuthServiceTest` | Registration, login, password hashing, JWT issuance | 5 |
| `TourDataTransferServiceTest` | Round-trip JSON export / import | 5 |
| `StatsServiceTest` | Aggregate metrics + transport breakdown | 5 |
| `OpenRouteLocationServiceTest` | Adapter delegation to ORS HTTP client | 3 |
| `JwtUtilTest` | Generation, parsing, signature, expiry | 5 |
| `ApiExceptionHandlerTest` | 400 / 404 / 409 / 500 error mapping | 7 |
| **Total** | | **55** |

### What we mock (and why)

- **Repositories** — mocked, because we test business logic, not JPA.
- **`JwtUtil`, `BCryptPasswordEncoder`** in `AuthServiceTest` — **not** mocked;
  cryptographic code must actually work.
- **`SecurityContextHolder`** — set up in `@BeforeEach` for every test that
  calls a user-scoped service, and cleared in `@AfterEach`.

### What we deliberately do not test

- **Controllers**: Spring MVC plumbing is covered by Spring's own tests.
- **Repositories**: method-name query derivation is Spring Data's contract.
- **iText PDF output**: binary comparison is brittle; reports are verified
  manually.

---

## 9. Known Design Constraints

| Constraint | Explanation |
|---|---|
| Single Angular component | Acceptable for this assignment; not production. No routing. |
| ORS API key required for route calc | Set `ORS_API_KEY` in `backend/.env`; map still renders without it. |
| PDF reports written to disk | Saved to `~/TourPlanner/reports/` before being streamed back. |
| Tour images stored on filesystem | Under `~/TourPlanner/images/`, referenced by absolute path. |
| No refresh token | JWT expires after 24 h; user must re-login. |

---

## 10. Sprint Log

### Sprint 1 — Project Setup
- Monorepo (`backend/`, `frontend/`), `.gitignore`, root `docker-compose.yml`.
- Spring Boot scaffold, JPA, Log4j2, dotenv loader.

### Sprint 2 — Core Domain
- `Tour` + `TourLog` entities, repositories, CRUD services, REST controllers.
- Angular scaffold, models, `ApiService`, list + form UI.

### Sprint 3 — Routing & Maps
- `OpenRouteService` client + `RouteController` endpoints.
- Leaflet map integration in Angular; polyline + markers.
- `.env`-loaded API key via `spring-dotenv`.

### Sprint 4 — Reports & I/O
- iText 7 `ReportService` for per-tour and summary PDFs.
- `TourDataTransferService` JSON import/export + `TourExportDto`.
- Frontend import/export buttons + tour image upload.

### Sprint 5 — Authentication
- `User` entity, BCrypt hashing, `JwtUtil`, `JwtAuthFilter`, `SecurityConfig`.
- `AuthController` register/login + Angular `AuthService` + `authInterceptor`.
- Per-user data isolation everywhere: `AuthContext.getCurrentUserId()`.

### Sprint 6 — Validation & Quality
- Jakarta Bean Validation on entities + `ApiExceptionHandler`.
- Frontend inline validation messages on all inputs.
- Full-text search with computed-attribute coverage.
- Computed attributes `popularity` + `childFriendliness`, served in `TourResponseDto`.

### Sprint 7 — Unique Feature, Tests, Docs
- `StatsService` + dashboard panel + reusable `StatCardComponent`.
- Moved ownership check into `TourLogService` and `TourDataTransferService`.
- Test suite expanded to **55** unit tests (services, security, error mapping, adapter).
- Documentation: UML, wireframes, protocol, time-tracking.

---

## 11. How to Run

### PostgreSQL
```bash
docker compose up -d
```

### Backend
```bash
./run_backend.sh           # auto-detects JDK 17, loads backend/.env, runs mvn spring-boot:run
```
Backend: `http://localhost:8081`.

### Frontend
```bash
./run_frontend.sh          # uses Node 22 via nvm, runs npm start
```
Frontend: `http://localhost:4200`.

### Everything at once
```bash
./run_project.sh           # starts Postgres (docker), backend, frontend
```

### Unit tests
```bash
cd backend && ./mvnw -o test
```
Expected: **Tests run: 55, Failures: 0, Errors: 0**.
