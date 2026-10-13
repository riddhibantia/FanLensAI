# FanLens AI — Complete Project Context

## 1. Executive Summary & Core Concept
**FanLens AI** is an Android-first companion app for private fantasy-cricket leagues among friends. Unlike public fantasy platforms focused on cash prize pools and large-scale contests, FanLens AI focuses on the social rivalry, live banter, and real-time comprehension of leaderboard shifts during a match.

The system features:
- **Private Mini-Leagues**: Create and join friend leagues using 6-character invite codes without monetary stakes, fees, or third-party fantasy platform passwords.
- **Deterministic, Versioned Scoring**: Ball-by-ball scoring computed strictly by the server and written to an idempotent, versioned ledger.
- **Companion Mascot (The Red Cricket Ball)**:
  - An optional, draggable floating overlay (`WindowManager`, `TYPE_APPLICATION_OVERLAY`) that appears across apps while watching cricket.
  - An Android home-screen widget (built with Jetpack Glance) reading from local cache.
- **Rivalry Intelligence (Server-Side LLM)**:
  - Natural-language explanations for leaderboard movements powered by Gemini.
  - Purely explanatory: computes zero scores, and every numeric claim is validated against server facts before presentation.

---

## 2. Hard Architectural Rules
1. **The LLM Never Computes Scores**: The AI model never computes or adjusts scores, ranks, deltas, or official events. It only explains data already calculated by the backend.
2. **Deterministic & Versioned Scoring**: Rulesets have explicit versions (e.g., `1.0`). Point deltas write to a ledger keyed by `(event_id, fantasy_team_id, player_id, ruleset_version)`. Reprocessing an event is an idempotent no-op; replaying the entire match produces identical totals.
3. **AI Output Is Grounded or Dropped**: Every LLM generation is verified against the JSON schema, match event IDs, and server facts. If an invented number, invalid event ID, timeout, or quota error occurs, the system falls back to a deterministic template.
4. **Two Companion Modes**:
   - `ALWAYS_REACTIVE`: Mascot briefly animates and shows non-blocking insights on selected triggers (rank swaps, gap thresholds), governed by quiet hours, cooldowns, and OS limits.
   - `TAP_TO_ACTIVATE`: Suppresses all unsolicited animations, popups, and proactive AI calls. Insights and data refreshes occur only upon user tap.
5. **Freshness Is Always Visible**: Standings and score cards display real-time freshness badges (`LIVE`, `DELAYED`, `STALE`, or `DEMO`) alongside a timestamp.
6. **No Client-Side Secrets**: All LLM API keys and backend credentials reside exclusively on the server.

---

## 3. Technology Stack

### Backend (`/backend`)
- **Runtime**: Python 3.10+
- **Framework**: FastAPI, Pydantic v2
- **Database & ORM**: SQLAlchemy 2.0, Alembic migrations, PostgreSQL (production) / SQLite (tests and local development)
- **AI Integration**: Google Gemini API with fallback template system
- **Testing**: `pytest` (77 unit and integration tests)

### Android App (`/android`)
- **Language & SDK**: Kotlin, minSdk 26, targetSdk 35
- **UI Framework**: Jetpack Compose, Material 3 (dark palette: deep navy/ink canvas, cricket ball red accents)
- **Architecture**: MVVM, Repository pattern, Service Locator (`ServiceLocator`)
- **Storage**:
  - **Jetpack DataStore**: Persistent user preferences (`companion_mode`, `refresh_policy`, quiet hours, motion level)
  - **Room Database**: Cache for standings snapshots, recent match events, and freshness indicators
- **Overlay**: Android `WindowManager` with `TYPE_APPLICATION_OVERLAY` and foreground service
- **Home Widget**: Jetpack Glance app widget (`ScoreWidget`)
- **Network**: OkHttp, `kotlinx.serialization` (null-tolerant partial PATCH handling)
- **Testing**: JUnit JVM tests (`testDebugUnitTest`)

---

## 4. End-to-End System Architecture

```
[ Live Match Data / Demo Script Feed ]
                 │
                 ▼
     [ Event Validation Adapter ]
                 │
                 ▼
       [ Scoring Engine v1 ]
                 │
                 ├─► [ Points Ledger Table ] (Idempotency Key)
                 │
                 ▼
      [ Standings Service ] ──► [ Snapshots Table ]
                 │
                 ├─► [ Freshness Evaluator ] (LIVE / DELAYED / STALE / DEMO)
                 │
                 ▼
      [ Insight Service ]
                 │
                 ▼
       [ Gemini LLM Client ]
                 │
                 ▼
      [ Fact & Schema Validator ]
                 │
        ┌────────┴────────┐
   (Valid Facts)    (Hallucination / Error)
        │                 │
        ▼                 ▼
   [ AI Text ]    [ Deterministic Template ]
        └────────┬────────┘
                 │
                 ▼
      [ FastAPI Endpoints ]
                 │
                 ▼
      [ Android Client Repositories ]
                 │
        ┌────────┴────────┐
        ▼                 ▼
  [ Room Cache ]   [ DataStore Preferences ]
        │                 │
        ├─────────────────┤
        │                 │
        ▼                 ▼
  [ Compose UI ]    [ Glance Widget ]    [ Mascot Overlay ]
```

---

## 5. Data Model (13 Core Tables)
- `users`: User identity, device tokens, display names.
- `leagues`: League records, hashed invite codes, expiry timestamps.
- `league_members`: Mapping of users to leagues (enforcing `(league_id, user_id)` unique constraint).
- `matches`: Match metadata, status (`SCHEDULED`, `LIVE`, `COMPLETED`), source, and timestamps.
- `scoring_rulesets`: Versioned ruleset configurations (`ruleset_version`, JSON rules).
- `fantasy_teams`: User squads linked to leagues, matches, and rulesets.
- `fantasy_team_players`: Roster selections, player roles (`BAT`, `BOWL`, `ALL`, `WK`), Captain/Vice-Captain flags.
- `match_events`: Ball-by-ball actions, unique `provider_event_id`, sequence, audit trails.
- `player_match_stats`: Cumulative player stats per match (runs, balls, wickets, boundaries, catches).
- `points_ledger`: Point delta ledger keyed by `(event_id, fantasy_team_id, player_id, ruleset_version)`.
- `standings_snapshots`: Historical standing records per scoring event (ranks, point totals, gaps, deltas).
- `user_preferences`: Server-synced user settings (companion mode, refresh policy, quiet hours, tone).
- `ai_insights`: Cached, validated AI explanations linked to specific match triggers.

---

## 6. Functional Requirements (PRD Coverage)

| Feature | Description | Implementation Details |
|---|---|---|
| **FR-01** | Private League Create/Join | `POST /v1/leagues` creates league with hashed expiring code; `POST /v1/leagues/{id}/join` joins. Device-token auth. |
| **FR-02** | Roster Management | 11 players per roster with role limits (min 3 BAT, min 3 BOWL, 1-4 ALL, 1-2 WK), 1 Captain (2x), 1 Vice-Captain (1.5x). |
| **FR-03** | Deterministic Scoring | Ruleset v1 points: Run (+1), Boundary (+1 extra), Six (+2 extra), Wicket (+25), Catch (+8), Stumping (+12), Duck (-5), Milestones (50 runs -> +8, 100 runs -> +16). |
| **FR-04** | Leaderboard & Gaps | Ranks ordered by points desc, team created_at asc, name asc. Calculates gap to leader and gap to rank ahead. |
| **FR-05** | Home-Screen Widget | Glance widget displaying user rank, total points, leader gap, and freshness badge directly from Room cache. |
| **FR-06** | Dismissible Mascot | Draggable floating cricket ball on screen via `WindowManager`. Can be dragged to dismiss target or disabled via in-overlay button. |
| **FR-07** | Mode Selection | `ALWAYS_REACTIVE` vs `TAP_TO_ACTIVATE`. Persisted in DataStore, controlling unsolicited mascot animations and insights. |
| **FR-08** | Grounded AI Explanations | Gemini client generates narrative insight from server facts. Fact validator drops or falls back to template if any number mismatches. |
| **FR-09** | Freshness Indicator | Freshness chip displayed across all score surfaces: `LIVE` (<30s), `DELAYED` (<5m), `STALE`, or `DEMO`. |
| **FR-10** | Event History Recap | Cursor-paginated match timeline feed (`GET /v1/matches/{id}/events?after=`) for ball-by-ball recap. |
| **FR-11** | Privacy & Rate Controls | 5-minute cooldown between proactive triggers, quiet hours checking, rate limits on join and AI explain endpoints. |
| **FR-12** | Simulated Demo Feed | `POST /v1/demo/matches/{id}/events` steps through scripted 10-ball timeline for standalone testing without external data providers. |

---

## 7. Current Project Status

### Completed
- **Backend**: Complete implementation of phases B1 through B9. 77 of 77 pytest unit and integration tests passing.
- **Android App**: Complete implementation of phases A1 through A5. Compose screens (`AuthScreen`, `LeagueScreen`, `RosterScreen`, `SettingsScreen`), Glance widget, and overlay service operational. JVM unit tests passing.
- **Testing**: Verified on physical Android device (Motorola edge 60, Android 16) over local network.

### Remaining for Production
- **Cloud Infrastructure**: Hosting FastAPI on a cloud provider and migrating local database to managed PostgreSQL.
- **Real-Time Push**: Integrating FCM (Firebase Cloud Messaging) or SSE (Server-Sent Events) for real-time proactive pushes.
- **Live Data Provider**: Integrating an external live cricket feed to replace the demo script feed.
- **Production AI Config**: Production Gemini API keys and quota management.
