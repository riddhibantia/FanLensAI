# Correctness audit — FanLens AI

Date: 2026-10-13. Scope: `android/` (33 Kotlin files, ~8.3k lines) and `backend/`
(72 Python files, ~4.9k lines).

## How this was produced, and how much to trust it

Two independent audits (Android data layer, backend scoring/AI) plus my own
verification of every finding listed as **Verified** below. Backend findings were
reproduced by executing code; Android findings were verified by reading the
call sites myself.

**Baseline is healthy:** `backend` suite is **83 passing**, not the 79 claimed in
`AGENTS.md`. Android's JVM tests were not run (no gradle run in this session), so
every Android claim below is from source inspection, not execution.

Severity reflects user impact, not effort to fix.

---

## Verified defects

### V1 — Duplicate event IDs are deduped globally, not per match (backend, severe)

`app/services/scoring/pipeline.py:58` calls
`find_existing_event(session, provider_event_id)`, which
(`app/services/event_adapter.py:59-62`) filters on `provider_event_id` **alone**.
That column is globally `unique=True` (`app/models/matches.py:26`).

Two leagues watching the same real match collide. The first ingest wins; the
second gets `{"status":"duplicate"}` and HTTP 201, and **that team scores zero
for the ball, permanently**. Providers reuse event IDs across feeds, so this is
reachable in normal operation, not a corner case.

The check also short-circuits before comparing payloads, so a genuine correction
(same ID, different runs) is silently dropped. The model already has
`superseded_by` and a `CORRECTED` status (`app/models/matches.py:31-33`) — the
schema supports corrections, but nothing writes them. The DB unique constraint
never gets a chance to help, because the pre-check returns first.

Fix: scope `find_existing_event` by `match_id` (and decide the correction
policy separately).

### V2 — `delta_vs_previous` can report the wrong number (backend, severe)

`app/services/standings_service.py:58-63` picks the "previous" batch with
`ORDER BY captured_at DESC LIMIT 1`. `captured_at` defaults to `func.now()`, so
two batches written in the same second tie, and the tie-break is arbitrary.

Confirmed by the auditor's execution: after three scoring events the deltas came
out `0, 2, 4` where they should be `0, 2, 2` — the third diffed against a
two-balls-old batch.

This is compounded by `app/api/v1/standings.py:36`, which writes a **new snapshot
on every GET**. A pure read therefore mutates what later reads report. This is a
wrong number on the leaderboard, not an internal detail.

Fix: order by an autoincrement id or an explicit per-batch sequence, not a
wall-clock timestamp.

### V3 — `setActiveLeague` wipes the active match (Android, severe)

`data/prefs/SettingsStore.kt:227` — `setActiveLeague(leagueId, matchId = "")`
defaults `matchId` to empty and **unconditionally overwrites** the stored value.

`createLeague` (`Repositories.kt:51`) and `joinLeague` (`:57`) both call it with
the default. So creating or joining a league erases whatever match was active.

Downstream consumers of `activeMatchId` then go blank: the overlay
(`CompanionOverlayService.kt:124,203`) and the widget (`ScoreWidget.kt:41`).

### V4 — Cross-league match contamination (Android, severe)

`ui/LeagueViewModel.kt:107` — `selectLeague` persists
`setActiveLeague(newLeagueId, _uiState.value.matchId)`, i.e. it writes the
**previous league's match id under the new league id**.

`refreshStandings(newLeague, oldLeagueMatch)` will 404 or return rows for the
wrong match. Related: `setMatchId` (`:296`) has no blank-league guard, so it can
persist an empty league id with a real match id.

### V5 — ALWAYS_REACTIVE is dead by default, and the overlay runs an untested gate (Android, severe)

Two different gates exist for the same decision:

- `ProactiveGate.canShowProactive` (`data/prefs/ProactiveGate.kt:16-37`) — checks
  companion mode, overlay, `aiEnabled`, trigger opt-in, quiet hours,
  notification category, cooldown, and gap threshold.
- `UserSettings.proactiveAllowed()` (`SettingsStore.kt:87-94`) — checks only
  companion mode, overlay, non-empty triggers, and quiet hours.

`CompanionOverlayService.kt:140` uses the **weak** one. `ProactiveGateTest.kt`
has nine tests for the **strong** one. `canShowProactive` is never called in
production code — so the gate that is tested is not the gate that ships.

Two consequences:

1. The overlay animates even when AI is disabled or the trigger wasn't opted in.
2. `ProactiveGate.kt:29` requires `notificationCategories.contains("INSIGHTS")`
   for proactive *animation*. That conflates notifications with overlay/motion,
   which the spec says must stay separate. Default is `setOf("SCORES")`, so
   **out of the box the gate is always false and ALWAYS_REACTIVE does nothing** —
   a direct violation of the spec's rule 4.

Also `ProactiveGate.kt:33-35`: the gap threshold is only enforced when
`gapDelta != null`; a null delta falls through to `return true`, firing the
reaction regardless of the user's configured threshold.

### V6 — A pure read mutates cached state (Android, moderate)

`ui/LeagueViewModel.kt:296` — `setMatchId` never triggers a refresh. Selecting a
match leaves the **previous match's cached rows on screen** until something else
forces a refresh.

### V7 — Stale state can never be cleared (Android, moderate)

`ui/LeagueViewModel.kt:79-88` — the settings collector uses
`.ifBlank { _uiState.value.X }`, so a deliberate clear never propagates. Once a
league/match is set in `_uiState`, it persists for the process lifetime. This
makes V3/V4 sticky.

---

## Lower severity

- **`Freshness.kt:16-20`** — when `updatedAt` is blank the label renders
  `"LIVE · no timestamp yet"`. The spec requires freshness *with* a timestamp;
  this presents a missing timestamp as a normal state.
- **`Freshness.kt:11-14`** — `normalize` returns `"NO DATA"`, a fifth state
  outside the four the spec defines.
- **`ProactiveGate.kt:30-32`** — the cooldown uses `now - lastShown < COOLDOWN`.
  A backward clock jump makes the difference negative, blocking the gate until
  wall-clock catches up. No clamp.
- **`LeagueViewModel.kt:55-57`** — `hasToken` uses `WhileSubscribed`, so a
  cold-start navigation decision can read `false` for a signed-in user and land
  on the auth screen.
- **`LeagueViewModel.kt:95-97`** — adopts `leagues.first()`, but the backend
  orders by `created_at`, so "first" is the *oldest* league, not the most recent.
- **`Repositories.kt:164-232`** — all ten `PreferencesRepository.set*` methods
  discard the `patchPreferences` `Result`. A server rejection silently diverges
  local from server state.
- **`ServiceLocator.kt:46`** — `onUnauthorized` is declared and invoked
  (`FanLensApi.kt:31`) but never assigned anywhere, so 401 recovery is dead code.
- **`Repositories.kt:108-127`** — `after` is a sequence number, but no cursor is
  persisted and `refreshEvents` returns a count. The event flow re-fetches from
  sequence 0. Mitigated by `@Upsert` on `providerEventId`, so this is wasteful
  rather than corrupting.
- **`FanLensApi.kt:49,64`** — ids are interpolated into path segments without
  URL-encoding.
- **`FanLensApi.kt:16,84`** — raw response bodies surface as user-facing error
  text via `ex.message`.
- **`FanLensDatabase.kt:91`** — `fallbackToDestructiveMigration` with
  `exportSchema = false`: any schema bump silently wipes the cache.
- **`FanLensDatabase.kt:52`** — `selfStanding` is declared and never called; the
  `isSelf` flag it depends on is also never read.

## Backend findings I did not independently re-verify

These came from the backend audit with execution evidence but I did not
reproduce them myself. Treat as strong leads, not confirmed.

- **Ruleset switching mixes currency in the ledger.** Rows are keyed by
  `ruleset_version`, but `roster_service.py:74` repoints `team.ruleset_id`
  without reprocessing history, so totals sum across rulesets. Reached through
  the public roster endpoint, which accepts any `ruleset_id`. This breaks the
  spec's "replaying the full log reproduces identical totals".
- **Floats decide bonus awards.** `pipeline.py:131-146` accumulates deltas as
  floats before `Decimal(str(...))`, and `engine.py:127,161` compare
  float-derived strike rate / economy against int thresholds — a 5-point bonus
  can flip at an exact boundary across platforms.
- **The `explain` endpoint passes the raw user question into the LLM prompt**
  (`insights.py:85-96`) with no length bound. The response is still validated, so
  invented numbers are caught; this is a prompt-injection surface, not a
  grounding break.

## Verified solid — do not re-audit

- **Membership enforcement.** `require_match_access` (`app/api/deps.py:34-46`)
  joins through `LeagueMember` and returns 403. The auditor probed private reads
  cross-tenant and cross-league and got 403 every time.
- **LLM grounding choke point.** Every LLM call routes through
  `validate_llm_output` at `insight_service.py:209`. I traced all three callers
  (`events.py:81`, `demo.py:84`, `insights.py:94`); there is no bypass. Any
  failure falls back to a deterministic template, and the template is the
  default value, not a fallback branch.
- **Ledger uniqueness.** `UniqueConstraint(event_id, fantasy_team_id,
  player_id, ruleset_version)` exists in both the model (`models/scoring.py:55`)
  and the migration.
- **Full-log replay is idempotent.** I wrote a probe that processes a 5-ball
  sequence and replays the whole log: totals, ledger row count, and event count
  were all unchanged on the second pass.
- **No money concepts** anywhere in the backend (models, schemas, routes,
  config) — `grep` for `₹|INR|prize|payout|entry_fee|stake|betting|wager`
  returns nothing.
- **Server-side gating.** `generate_insight` enforces `ai_enabled`, companion
  mode, and trigger membership in the backend, not just the app.
- **No secrets in the Android source.** The LLM key stays server-side.

## Caveats

- The validator checks that each number *appears somewhere* in the facts, not
  that it is attached to the correct claim. I confirmed that
  `"Your rival has 428 points and you trail by 428"` passes when the real gap is
  `0`, because `428` is in the allowed set. Regex grounding cannot close this;
  it needs claim-level checks. This is a known limitation of the approach rather
  than a coding error.
- Android findings are unexecuted. `OverlayStateTest.kt` and the other JVM tests
  have not been run.

## Repo state note

`backend/` has 18 files with uncommitted modifications that predate this audit
(the repo is a fresh clone with no commits since). I did not author or alter
them and left them untouched. `AGENTS.md` says 76 tests; the suite is 83.
