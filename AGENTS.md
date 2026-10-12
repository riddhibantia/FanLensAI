# FanLens AI — Project Context

## What this is
FanLens AI is an Android-first companion for private fantasy-cricket leagues between friends. Users create/join leagues with invite codes, register rosters for a match, and watch fantasy points and ranks through a red cricket-ball mascot (optional cross-app overlay + home-screen widget). A server-side LLM explains why standings changed ("Rivalry Intelligence").

**Repo state:** backend (`backend/`, FastAPI, 76 tests) plus Android app (`android/`, Kotlin/Compose, JVM unit tests) implement the PRD/TRD. Source of truth:
- `FanLens_AI_PRD_TRD_Bundle/FanLens_AI_PRD.md` — product requirements (v1.0, 10 Oct 2026)
- `FanLens_AI_PRD_TRD_Bundle/FanLens_AI_TRD.md` — technical requirements (v1.0, 10 Oct 2026)
- The `.docx` files in that folder are styled exports of the same two documents.
- `IMPLEMENTATION_PLAN.md` (repo root) — the detailed build plan derived from PRD + TRD.

## Hard rules — never break these
1. **Backend computes all scores, ranks, deltas and gaps.** The LLM never produces or updates a score, rank, or official event. It only explains numbers the server already computed.
2. **Scoring is deterministic and versioned.** Every ruleset carries an explicit version; point deltas are written to a ledger keyed `(event_id, fantasy_team_id, player_id, ruleset_version)`. Re-processing an event must be a no-op (idempotent). Replaying the full event log must reproduce identical totals.
3. **AI output is validated or dropped.** Validate the JSON schema, every numeric claim against server facts, and every referenced event ID. On any failure (bad JSON, invented number, timeout, quota), fall back to a deterministic template. A template fallback is a normal state, not an error to hide.
4. **Two companion modes, user-chosen, persisted, switchable any time:**
   - `ALWAYS_REACTIVE` — mascot may briefly react to selected meaningful events and show non-blocking insights, subject to settings, quiet hours, cooldowns and OS limits.
   - `TAP_TO_ACTIVATE` — zero unsolicited animations or AI insights. Tap opens the panel and can refresh data or request an explanation.
5. **Separate settings, never conflated:** `overlay_enabled`, score refresh policy (`AUTO` / `ON_OPEN` / `MANUAL`), proactive trigger categories, gap threshold, quiet hours, motion level, widget fields, notification categories, AI enablement, tone. Tap-only mode never disables scoring or score data.
6. **Freshness is always visible.** Every score display shows live / delayed / stale / demo with a timestamp.
7. **No money contests, betting, entry fees or payouts. No scraping. No external fantasy-platform passwords. No secrets in the Android app** — the LLM key lives on the server only.
8. **Overlay is best-effort.** The permission is user-granted; background/service/refresh are OS-constrained. Always provide hide/dismiss plus widget and in-app fallbacks. Never promise instant reaction to every ball.

## Stack (from TRD §2)
- **Android:** Kotlin, Jetpack Compose, MVVM + repositories, DataStore (preferences), Room (cache). Overlay: `WindowManager` / `TYPE_APPLICATION_OVERLAY` + foreground service where Android rules require it. Widget: AppWidgetProvider or Jetpack Glance — the widget is separate from the overlay.
- **Backend:** Python, FastAPI + Pydantic, SQLAlchemy + Alembic.
- **Database:** managed PostgreSQL.
- **AI:** server-side Gemini (or equivalent) via managed API key.
- **Updates:** foreground polling or SSE for the prototype; FCM for important-change notifications. Push is never the authoritative event log.
- **Deploy:** managed API host + managed Postgres, separate demo and production credentials.

## Components (TRD §3)
Event adapter · Scoring engine · League service · Standings service · Insight service · Android controller (settings/overlay lifecycle) · Widget renderer.

## Data model (TRD §6)
Tables: `users`, `leagues`, `league_members`, `matches`, `scoring_rulesets`, `fantasy_teams`, `fantasy_team_players`, `match_events`, `player_match_stats`, `points_ledger`, `standings_snapshots`, `user_preferences`, `ai_insights`.
Constraints: unique `(league_id, user_id)`; stable unique event IDs; idempotency key per event/team/player/ruleset; explicit ruleset version; match status + source freshness; membership check on every private read; audit trail for event corrections.

## API surface (TRD §7)
`POST /v1/leagues` · `POST /v1/leagues/{id}/join` · `GET /v1/leagues/{id}` · `GET /v1/leagues/{id}/standings?match_id=` · `POST /v1/leagues/{id}/rosters` · `GET /v1/matches/{id}/events?after=` · `POST /v1/demo/matches/{id}/events` · `GET /v1/leagues/{id}/insights` · `POST /v1/leagues/{id}/insights/explain` · `PATCH /v1/me/preferences`

## Feature priority (PRD §8)
P0: FR-01 private league create/join · FR-02 roster management · FR-03 deterministic versioned scoring · FR-04 leaderboard and gaps · FR-05 home-screen widget · FR-06 dismissible floating mascot · FR-07 user-selectable reactive mode · FR-08 grounded AI explanation · FR-09 freshness indicator · FR-11 privacy/notification controls · FR-12 simulated demo fallback.
P1: FR-10 recent event history / recap.

## Definition of done (TRD §12)
- [ ] Replaying a simulated event does not double-count points.
- [ ] Mode switch persists across app restart.
- [ ] Always Reactive surfaces configured insights where permitted; Tap to Activate suppresses all proactive mascot and AI behavior.
- [ ] Widget/app stay useful when overlay permission is denied.
- [ ] Freshness visible; offline / AI / provider failure has a fallback.
- [ ] AI numeric claims match server facts, or a template is used.
- [ ] No unsupported external fantasy-platform integration is claimed.

## Working agreements
- The PRD/TRD in `FanLens_AI_PRD_TRD_Bundle/` are the requirements of record. Code follows them; when the two disagree with this file, the docs win — update this file, not the intent.
- The 5-day schedule lives in TRD §11 and is expanded in `IMPLEMENTATION_PLAN.md` §6.
- Before claiming work is done, run the checks listed in `IMPLEMENTATION_PLAN.md` §7.
