# FanLens AI — Technical Requirements Document (TRD)

**Version:** 1.0 · 10 October 2026  
**Status:** Working draft  
**Platform:** Native Android first; managed backend; no Docker required for prototype

## 1. Architecture principle
**Authoritative event → deterministic scoring → persisted ledger/standings → optional grounded AI explanation → user-controlled presentation.**

## 2. Recommended stack
- **Android:** Kotlin, Jetpack Compose, MVVM/repositories, DataStore for preferences, Room for optional cache.
- **Overlay:** `WindowManager` / `TYPE_APPLICATION_OVERLAY`, with required permission; use foreground service only where current Android rules permit/require it.
- **Home-screen widget:** AppWidgetProvider or Jetpack Glance. A widget is distinct from the cross-app overlay.
- **Backend:** FastAPI + Pydantic, SQLAlchemy/Alembic.
- **Database:** Managed PostgreSQL (e.g. Supabase).
- **AI:** Server-side Gemini or equivalent API; never embed provider keys in the app.
- **Updates:** Foreground polling or SSE for prototype; FCM can notify about important changes. Do not treat push as the authoritative event log.
- **Deployment:** Managed API host + managed Postgres; separate demo and production credentials.

## 3. Component responsibilities
| Component | Responsibility |
|---|---|
| Event adapter | Normalizes live or simulated events with stable IDs and timestamps. |
| Scoring engine | Applies versioned rules deterministically and writes idempotent point deltas. |
| League service | Membership, invite codes, roster validation and permissions. |
| Standings service | Totals, ranks, points gaps and snapshots. |
| Insight service | Trigger detection, LLM call, validation, cache and fallback template. |
| Android controller | Settings, overlay lifecycle, position, visibility and tap behavior. |
| Widget renderer | Cached glanceable summary with freshness status and deep link. |

## 4. User-controlled mode logic
Settings store `companion_mode = ALWAYS_REACTIVE | TAP_TO_ACTIVATE`. Separate settings govern `overlay_enabled`, score refresh policy, proactive trigger categories, gap threshold, quiet hours, motion level, widget fields, notification categories and AI enablement.

- **Always Reactive:** when overlay is enabled, a selected meaningful event may trigger a brief animation and non-blocking insight, subject to settings and OS limits.
- **Tap to Activate:** no unsolicited animations/AI insights; tapping opens the panel and can refresh stale data or request an explanation.
- Scoring continues independently of mode. Score refresh policy remains explicit and separate.

**Android caveat:** overlay permission is user-granted. Background execution, foreground service starts and widget refresh are OS-constrained, so “always reactive” is best-effort and must never be sold as instantaneous reaction to every ball. Provide hide/dismiss and in-app fallback.

## 5. Event-to-insight flow
1. Receive provider/demo event with stable ID, match ID, sequence and source timestamp.
2. Validate and deduplicate.
3. Persist event and calculate player/team deltas with the ruleset version.
4. Write ledger in a transaction and recalculate affected totals/ranks.
5. Detect meaningful triggers.
6. Generate AI insight only if the user mode/preferences permit, or the user explicitly asks.
7. Return authoritative standings plus validated insight/status.
8. Log event/ruleset IDs and validation outcome without secrets.

## 6. Data model
Core tables: `users`, `leagues`, `league_members`, `matches`, `scoring_rulesets`, `fantasy_teams`, `fantasy_team_players`, `match_events`, `player_match_stats`, `points_ledger`, `standings_snapshots`, `user_preferences`, `ai_insights`.

Important constraints: unique `(league_id, user_id)` membership; stable unique event IDs; idempotency key per event/team/player/ruleset; explicit ruleset version; match status/source freshness; league-membership checks on all private reads. Preserve an audit trail for event corrections.

## 7. API sketch
- `POST /v1/leagues` — create league.
- `POST /v1/leagues/{id}/join` — join through invite code.
- `GET /v1/leagues/{id}` — league summary.
- `GET /v1/leagues/{id}/standings?match_id=...` — totals, ranks, deltas, freshness.
- `POST /v1/leagues/{id}/rosters` — register/update roster.
- `GET /v1/matches/{id}/events?after=...` — cursor-based event history.
- `POST /v1/demo/matches/{id}/events` — demo-only event injection.
- `GET /v1/leagues/{id}/insights` — recent insights.
- `POST /v1/leagues/{id}/insights/explain` — explain event/delta.
- `PATCH /v1/me/preferences` — server-synced preferences as needed.

## 8. AI grounding and fallback
Pass verified event summaries, event IDs, server-calculated deltas/ranks, ruleset context, and only necessary member display names. Ask for structured JSON. Validate schema, numbers, IDs and length. Safest MVP approach: template numeric statements and let the LLM write only the explanatory clause. If AI fails, show a deterministic template. AI cannot update scores or rankings.

Triggers: rank change, configured gap threshold, followed-player impact, match end, and direct user question. Cache/deduplicate and apply per-user/per-league rate limits.

## 9. Security and privacy
- Membership/role authorization on every private endpoint.
- Store invite codes as hashes and expire them.
- HTTPS, managed DB controls, least-privilege secrets.
- No external fantasy-platform passwords; no scraping or unauthorized sync.
- Treat user text as untrusted prompt content. Rate-limit AI and invite endpoints.
- Keep overlay/notification/AI choices user controlled; define deletion and retention before production.

## 10. Testing
- Unit: scoring, captain multipliers, roster constraints, tie-breakers and triggers.
- Integrity: duplicate/out-of-order/corrected event; replay yields same totals.
- API: membership, invite expiry, unauthorized reads, rate limiting and fallback.
- Android: mode persistence, permission denied/revoked, app lifecycle, dismiss/hide, reduced motion, stale-data state.
- AI: malformed JSON, invented number/event, prompt injection, quota/timeout and template fallback.
- Demo: event → points → standings → Always Reactive insight; switch to Tap to Activate and verify no proactive insight.

## 11. Five-day plan
| Day | Deliverable |
|---|---|
| 1 | Seeded league, roster data, deterministic scoring, unit fixtures. |
| 2 | Compose interface, red-ball overlay, settings and widget/in-app fallback. |
| 3 | Simulated event feed, idempotent updates, visible freshness. |
| 4 | AI Rivalry Intelligence, trigger preferences and grounded fallbacks. |
| 5 | QA, deployment, privacy checks and complete demo walkthrough. |

## 12. Definition of done
- Replaying a simulated event does not double-count points.
- User can switch modes; preference persists across restart.
- Always Reactive can surface configured insights where permitted; Tap to Activate suppresses proactive mascot and AI behavior.
- Widget/app remain useful when overlay permission is denied.
- Freshness is visible; offline/AI/provider failure has a fallback.
- AI numeric claims match server facts or a template is used.
- No unsupported external fantasy-platform integration is claimed.
