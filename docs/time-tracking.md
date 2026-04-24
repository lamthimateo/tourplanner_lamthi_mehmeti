# Tour Planner — Time Tracking

**Course:** SWEN 2 — Software Engineering 2  
**Team:** Lamthi, Mehmeti  
**Semester:** SS 2026

---

## Weekly Summary

| Week | Dates | Hours | Focus |
|------|-------|-------|-------|
| 1 | 2026-02-23 – 2026-03-01 | 6 h | Project setup, Spring Boot scaffold, Postgres via Docker Compose |
| 2 | 2026-03-02 – 2026-03-08 | 8 h | Tour + TourLog entities, basic CRUD endpoints, Angular scaffold |
| 3 | 2026-03-09 – 2026-03-15 | 10 h | OpenRouteService integration, Leaflet map, route calculation |
| 4 | 2026-03-16 – 2026-03-22 | 9 h | iText 7 PDF reports, import/export (TourDataTransferService) |
| 5 | 2026-03-23 – 2026-03-29 | 11 h | JWT authentication, Spring Security, AuthService, frontend login |
| 6 | 2026-03-30 – 2026-04-05 | 10 h | Validation, computed attributes, search, per-user isolation |
| 7 | 2026-04-06 – 2026-04-10 | 12 h | Unique feature (Stats), first wave of unit tests, bug fixes, documentation |
| 8 | 2026-04-20 – 2026-04-23 | 6 h | Security hardening (log ownership), extra tests (stats, jwt, advice, adapter), docs & README polish |
| **Total** | | **72 h** | |

---

## Detailed Task Log

### Week 1 — Project Setup

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-02-23 | Monorepo setup, `.gitignore`, README | 1.0 h | All |
| 2026-02-24 | Spring Boot project init, `pom.xml` dependencies | 1.5 h | Lamthi |
| 2026-02-25 | `application.properties`, `DotenvLoader`, Postgres docker-compose | 1.5 h | Mehmeti |
| 2026-02-26 | Angular 17 project init, `angular.json`, `app.config.ts` | 1.5 h | Mehmeti |
| 2026-02-27 | Architecture discussion, monorepo decision | 0.5 h | All |

### Week 2 — Core Domain

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-03-02 | `Tour` entity, `TourRepository`, basic CRUD service | 2.0 h | Lamthi |
| 2026-03-03 | `TourLog` entity, `TourLogRepository`, CRUD service | 2.0 h | Mehmeti |
| 2026-03-04 | `TourController`, `TourLogController` REST endpoints | 1.5 h | Lamthi |
| 2026-03-05 | Angular models (`tour.ts`, `tour-log.ts`), `ApiService` | 1.5 h | Mehmeti |
| 2026-03-06 | Angular tour list + form UI | 1.0 h | Mehmeti |

### Week 3 — Routing & Maps

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-03-09 | `OpenRouteService` geocoding integration | 2.5 h | Lamthi |
| 2026-03-10 | `RouteController`, ORS route endpoint | 1.5 h | Lamthi |
| 2026-03-11 | Leaflet map integration in Angular | 2.0 h | Mehmeti |
| 2026-03-12 | Route polyline drawing, marker rendering | 2.0 h | Mehmeti |
| 2026-03-13 | `.env` ORS API key loading via `DotenvLoader` | 1.5 h | Mehmeti |
| 2026-03-14 | End-to-end route calculation test | 0.5 h | All |

### Week 4 — Reports & Import/Export

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-03-16 | iText 7 dependency, `ReportService` scaffold | 1.5 h | Mehmeti |
| 2026-03-17 | Tour PDF report layout and generation | 2.5 h | Mehmeti |
| 2026-03-18 | Summary PDF report (all tours table) | 1.5 h | Mehmeti |
| 2026-03-19 | `TourDataTransferService` export to JSON | 1.5 h | Lamthi |
| 2026-03-20 | Import from JSON, `TourExportDto` | 1.5 h | Lamthi |
| 2026-03-21 | Frontend import/export buttons and file input | 0.5 h | Mehmeti |

### Week 5 — Authentication

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-03-23 | `User` entity, `UserRepository`, BCrypt setup | 1.5 h | Lamthi |
| 2026-03-24 | `JwtUtil` (JJWT 0.11.5), token generation/validation | 2.0 h | Lamthi |
| 2026-03-25 | `JwtAuthFilter`, `SecurityConfig`, CORS setup | 2.0 h | Lamthi |
| 2026-03-26 | `AuthService`, `AuthController` register/login | 1.5 h | Mehmeti |
| 2026-03-27 | `AuthContext`, per-user data isolation in TourService | 1.5 h | Mehmeti |
| 2026-03-28 | Angular `AuthService`, localStorage token storage | 1.5 h | Mehmeti |
| 2026-03-29 | Angular `authInterceptor`, login/register screen | 1.5 h | Mehmeti |

### Week 6 — Validation & Quality

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-03-30 | Jakarta validation on `Tour` + `TourLog` entities | 1.5 h | Mehmeti |
| 2026-03-31 | `ApiExceptionHandler` for validation errors | 1.5 h | Mehmeti |
| 2026-04-01 | Full-text search with computed attribute filtering | 2.0 h | Lamthi |
| 2026-04-02 | Computed attributes: `popularity`, `childFriendliness` | 1.5 h | Lamthi |
| 2026-04-03 | `TourResponseDto`, map computed fields in controller | 1.0 h | Lamthi |
| 2026-04-04 | Frontend validation messages, field name fixes | 2.0 h | Mehmeti |
| 2026-04-05 | Fixed `zone.js` polyfill, node_modules rebuild | 0.5 h | Mehmeti |

### Week 7 — Unique Feature, Tests & Docs

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-04-06 | `StatsService`, `StatsController`, `StatsDto` | 2.0 h | Lamthi |
| 2026-04-06 | Statistics Dashboard panel in Angular | 1.5 h | Mehmeti |
| 2026-04-07 | `TourServiceCrudTest` (9 tests) | 1.5 h | Mehmeti |
| 2026-04-07 | `TourLogServiceTest` (7 tests) | 1.5 h | Mehmeti |
| 2026-04-08 | `AuthServiceTest` (5 tests) | 1.0 h | Mehmeti |
| 2026-04-08 | Fixed `TourServiceSearchTest` (auth SecurityContextHolder) | 0.5 h | Mehmeti |
| 2026-04-09 | Angular template parse error fix (`toggleStats()`) | 0.5 h | Mehmeti |
| 2026-04-09 | Javadoc comments — all backend files | 2.0 h | Lamthi |
| 2026-04-10 | JSDoc comments — all frontend files | 1.5 h | Mehmeti |
| 2026-04-10 | Documentation: protocol, UML, wireframes | 1.0 h | All |

### Week 8 — Final Polish

| Date | Task | Time | Person |
|------|------|------|--------|
| 2026-04-20 | Security hardening: `TourLogService` now enforces tour ownership | 1.0 h | Lamthi |
| 2026-04-20 | `TourDataTransferService` stamps current user on import, scopes export | 0.5 h | Lamthi |
| 2026-04-21 | New `StatsServiceTest`, `JwtUtilTest`, `OpenRouteLocationServiceTest` | 1.5 h | Mehmeti |
| 2026-04-21 | New `ApiExceptionHandlerTest` (REST error mapping) | 0.5 h | Mehmeti |
| 2026-04-22 | `run_backend.sh` auto-detects JDK 17; cleaned up `.env.example` | 0.5 h | Mehmeti |
| 2026-04-22 | Protocol rewrite (TOC, architecture diagram, accurate lessons) | 1.0 h | All |
| 2026-04-23 | README + time-tracking + wireframes polish, final test run | 1.0 h | All |

---

## Effort by Area

| Area | Hours | % |
|------|-------|---|
| Backend (entities, services, repositories) | 22 h | 31% |
| Security (JWT, Spring Security, auth, ownership hardening) | 11 h | 15% |
| Frontend (Angular UI, maps) | 16 h | 22% |
| Tests (55 unit tests across 9 classes) | 9 h | 12% |
| External integrations (ORS, iText, PDF) | 7 h | 10% |
| Documentation & comments | 7 h | 10% |
| **Total** | **72 h** | **100%** |

---

## Effort by Person

| Person  | Hours | Primary Areas |
|---------|-------|---------------|
| Lamthi  | 37 h  | Backend entities & services, CRUD, search, ORS/geocoding, JWT & Spring Security, Stats feature, security hardening, Javadoc |
| Mehmeti | 35 h  | TourLog service, ReportService (iText), Angular UI + Leaflet map, auth frontend, validation, import/export, unit tests, run scripts, JSDoc |
