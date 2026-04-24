# Tour Planner — UI Wireframes

All wireframes represent the dark-themed Angular single-page application at `http://localhost:4200`.

---

## Screen 1 — Login / Register

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                                                         │
│              ┌───────────────────────────┐              │
│              │  Login                    │              │
│              │                           │              │
│              │  Username                 │              │
│              │  ┌─────────────────────┐  │              │
│              │  │ Username (min 3)    │  │              │
│              │  └─────────────────────┘  │              │
│              │                           │              │
│              │  Password                 │              │
│              │  ┌─────────────────────┐  │              │
│              │  │ ••••••••••          │  │              │
│              │  └─────────────────────┘  │              │
│              │                           │              │
│              │  ┌─────────────────────┐  │              │
│              │  │       Login         │  │              │
│              │  └─────────────────────┘  │              │
│              │                           │              │
│              │    No account? Register   │              │
│              └───────────────────────────┘              │
│                                                         │
└─────────────────────────────────────────────────────────┘

State: isRegistering=false  →  button says "Login", link says "No account? Register"
State: isRegistering=true   →  button says "Register", link says "Already have an account? Login"

Validation (shown below each field on submit):
  • Username < 3 chars  → "Username must be at least 3 characters"  (red)
  • Password < 4 chars  → "Password must be at least 4 characters"  (red)
  • Backend error       → red banner below button
```

---

## Screen 2 — Main App (tour selected)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│ ┌──────── SIDEBAR (360px) ──────────┐ ┌──────── MAIN AREA ─────────────────────┐
│ │                                   │ │                                        │
│ │ Logged in as bob      [Logout]    │ │ ┌── TOUR FORM ────────────────────────┐ │
│ │                                   │ │ │                                    │ │
│ │ ┌─────────────────────────────┐   │ │ │  Name *          Transport *       │ │
│ │ │ 🔍 Search tours...          │   │ │ │  ┌────────────┐  ┌────────────┐   │ │
│ │ └─────────────────────────────┘   │ │ │  │ Alpine...  │  │ hiking   ▼│   │ │
│ │                                   │ │ │  └────────────┘  └────────────┘   │ │
│ │ [+ New tour] [Statistics]         │ │ │                                    │ │
│ │ [Import]                          │ │ │  Description                       │ │
│ │                                   │ │ │  ┌──────────────────────────────┐  │ │
│ │ ┌─────────────────────────────┐   │ │ │  │ A scenic mountain route...  │  │ │
│ │ │ Alpine Trek          ← active│  │ │ │  └──────────────────────────────┘  │ │
│ │ │ Vienna → Salzburg           │  │ │ │                                    │ │
│ │ │ hiking · 120km · 480min     │  │ │ │  From *            To *            │ │
│ │ │ ★ pop:3 · child:7.2/10     │  │ │ │  ┌────────────┐  ┌────────────┐   │ │
│ │ └─────────────────────────────┘   │ │ │  │ Vienna     │  │ Salzburg   │   │ │
│ │                                   │ │ │  └────────────┘  └────────────┘   │ │
│ │ ┌─────────────────────────────┐   │ │ │                                    │ │
│ │ │ City Bike Tour              │  │ │ │  Distance (km)    Est. time (min)  │ │
│ │ │ Berlin → Potsdam            │  │ │ │  ┌────────────┐  ┌────────────┐   │ │
│ │ │ bicycle · 45km · 180min    │  │ │ │  │ 120        │  │ 480        │   │ │
│ │ │ ★ pop:1 · child:5.0/10    │  │ │ │  └────────────┘  └────────────┘   │ │
│ │ └─────────────────────────────┘   │ │ │                                    │ │
│ │                                   │ │ │  Popularity: 3 log(s)              │ │
│ │                                   │ │ │  Child-friendliness: 7.2/10        │ │
│ │                                   │ │ │                                    │ │
│ │                                   │ │ │  [Save]         [Delete]           │ │
│ │                                   │ │ │  [Calculate route] [Export JSON]   │ │
│ │                                   │ │ │  [Tour PDF]    [Summary PDF]       │ │
│ │                                   │ │ └────────────────────────────────────┘ │
│ │                                   │ │                                        │
│ │                                   │ │ ┌── MAP ─────────────────────────────┐ │
│ │                                   │ │ │                                    │ │
│ │                                   │ │ │   [OpenStreetMap tile layer]       │ │
│ │                                   │ │ │   ● Vienna ────────────── ● Salzb  │ │
│ │                                   │ │ │                                    │ │
│ │                                   │ │ └────────────────────────────────────┘ │
│ │                                   │ │                                        │
│ │                                   │ │ ┌── TOUR LOGS ───────────────────────┐ │
│ │                                   │ │ │ Tour logs              [+ New log] │ │
│ │                                   │ │ └────────────────────────────────────┘ │
│ │                                   │ │                                        │
│ │                                   │ │ ┌─────────────────────────────────┐   │
│ │                                   │ │ │ 2026-03-15                      │   │
│ │                                   │ │ │ Great weather, tough climb       │   │
│ │                                   │ │ │                    ★ 8/10        │   │
│ │                                   │ │ │                    45km · 210min │   │
│ │                                   │ │ │                    difficulty 4/5│   │
│ │                                   │ │ │ [Delete]                         │   │
│ │                                   │ │ └─────────────────────────────────┘   │
└─┴───────────────────────────────────┴─┴────────────────────────────────────────┘
```

---

## Screen 3 — Log Edit Form (expanded below tour form)

```
┌── LOG FORM ──────────────────────────────────────────────┐
│                                                          │
│  Date *                     Rating (0-10) *             │
│  ┌─────────────────────┐   ┌──────────────────────┐    │
│  │ 2026-03-15          │   │ 8                    │    │
│  └─────────────────────┘   └──────────────────────┘    │
│                                                          │
│  Comment *                                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Great weather, tough climb on the last section   │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Difficulty (1-5) *         Total time (min)            │
│  ┌─────────────────────┐   ┌──────────────────────┐    │
│  │ 4                   │   │ 210                  │    │
│  └─────────────────────┘   └──────────────────────┘    │
│                                                          │
│  Total distance (km)                                    │
│  ┌─────────────────────┐                               │
│  │ 45                  │                               │
│  └─────────────────────┘                               │
│                                                          │
│  [Save log]             [Cancel]                        │
└──────────────────────────────────────────────────────────┘
```

---

## Screen 4 — Statistics Dashboard Panel

Shown when user clicks "Statistics" button. Appears above the tour form / empty state.

```
┌── STATISTICS DASHBOARD ──────────────────────────────────────────┐
│                                                                  │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │    2    │ │    4    │ │  165 km  │ │   9.5 h  │ │  7.4   │ │
│  │  Tours  │ │  Logs   │ │  Total   │ │  Total   │ │Avg rat.│ │
│  │         │ │         │ │  dist.   │ │   time   │ │        │ │
│  └─────────┘ └─────────┘ └──────────┘ └──────────┘ └────────┘ │
│                                                                  │
│  By transport type:                                             │
│  ┌───────────┬───────┬──────┬──────────┬────────────┐          │
│  │ Type      │ Tours │ Logs │ Avg dist │ Avg rating │          │
│  ├───────────┼───────┼──────┼──────────┼────────────┤          │
│  │ hiking    │   1   │   3  │  55 km   │    8.0     │          │
│  │ bicycle   │   1   │   1  │  45 km   │    6.0     │          │
│  └───────────┴───────┴──────┴──────────┴────────────┘          │
└──────────────────────────────────────────────────────────────────┘
```

---

## Screen 5 — Empty State (no tour selected)

```
┌── MAIN AREA ──────────────────────────────────────────────┐
│                                                           │
│  ┌───────────────────────────────────────────────────┐   │
│  │  Select a tour on the left (or create a new one). │   │
│  └───────────────────────────────────────────────────┘   │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

## Responsive Notes

- Sidebar is fixed at 360px; main area fills remaining width
- Map div is 360px tall with rounded corners
- All inputs are full-width within their grid cell
- Two-column grid used for paired fields (Name/Transport, From/To, Distance/Time, action buttons)
- On small screens (< 700px): layout would stack vertically (not currently implemented)

---

## Color Palette

| Element | Value |
|---|---|
| Background | `#0b0e14` |
| Card background | `rgba(255,255,255,0.06)` |
| Card border | `rgba(255,255,255,0.08)` |
| Text (primary) | `#e6e6e6` |
| Text (muted) | `rgba(255,255,255,0.75)` |
| Input background | `rgba(0,0,0,0.25)` |
| Active outline | `rgba(255,255,255,0.25)` |
| Error / invalid | `#e55` |
