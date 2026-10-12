# FanLens AI — Product Requirements Document (PRD)

**Version:** 1.0 · 10 October 2026  
**Status:** Working draft  
**Target:** Android-first private fantasy-cricket league companion

## 1. Executive summary
FanLens AI lets friends compete in private fantasy-cricket leagues, track each member’s fantasy points, and see leaderboard changes through a persistent red cricket-ball companion. AI explains why meaningful changes happened using verified match and scoring data.

**Principle:** Private-league competition comes first; the score widget makes it glanceable; the mascot makes it personal; AI explains why changes matter.

## 2. Problem
Fans repeatedly switch between fantasy score screens, match information, and group chat to understand who is leading. Raw point changes often do not explain which event caused the change.

## 3. Goals
- Make private-league competition easy to follow during a match.
- Show fantasy points, rank, point gaps, and key changes at a glance.
- Explain important changes with grounded AI.
- Let every user control whether the companion is proactive or only responds when tapped.
- Show freshness and distinguish live, delayed, stale, and demo data.

## 4. Core experience
1. Create a private league and invite friends.
2. Register each member’s fantasy roster for a selected match.
3. Calculate points through a versioned deterministic scoring ruleset.
4. Compare standings and recent point changes.
5. Keep the red cricket-ball companion available as an optional Android overlay and home-screen widget.
6. Ask AI why a member overtook another, what changed, or who contributed most.

## 5. MVP requirements
- Private league creation/joining with invite code/link.
- Member roster entry and scoring-ruleset selection.
- Deterministic scoring, idempotent event processing, standings and point history.
- Android overlay and separate home-screen widget.
- Always Reactive and Tap to Activate modes.
- User controls for overlay visibility, refresh policy, trigger categories, gap threshold, quiet hours, motion and notifications.
- AI explanations based on verified points/ranks/events; deterministic fallback if AI fails.
- A simulated match-event feed so the demo does not depend on live API access.

## 6. Companion personalization
The user chooses the mode in Settings; it must persist and be switchable at any time.

| Setting | Behavior |
|---|---|
| **Always Reactive** | The companion may react briefly to selected meaningful league events and show non-blocking insights, subject to quiet hours, cooldowns, permissions and Android OS limits. |
| **Tap to Activate** | No unsolicited mascot animations or AI insights. A tap opens the panel and can refresh current details or request an explanation. |
| Overlay visibility | Independent on/off switch. |
| Score refresh | Separate Auto / On Open / Manual control. Tap-only does not inherently mean score data is deleted or disabled. |
| Trigger categories | Rank changes, followed-player impact, points-gap threshold, match end. |
| Quiet hours / motion | Suppress proactive reactions; offer reduced motion/off. |

## 7. AI use case: Rivalry Intelligence
- Detect rank swaps, configured gap changes, meaningful followed-player contributions, and match completion.
- Scoring and ranks are calculated by backend code, never by the LLM.
- Send verified event IDs, point deltas, ranks, ruleset context, and user tone preferences to the LLM.
- Validate structured output and numeric claims; otherwise use a template.
- Cache/deduplicate insights and apply a cooldown to prevent spam.

## 8. Functional requirement summary
| ID | Requirement | Priority |
|---|---|---|
| FR-01 | Private league create/join | P0 |
| FR-02 | Team/roster management | P0 |
| FR-03 | Deterministic versioned scoring | P0 |
| FR-04 | Leaderboard and point gaps | P0 |
| FR-05 | Home-screen score widget | P0 |
| FR-06 | Dismissible floating mascot | P0 |
| FR-07 | User-selectable reactive mode | P0 |
| FR-08 | Grounded AI explanation | P0 |
| FR-09 | Freshness/status indicator | P0 |
| FR-10 | Recent event history / recap | P1 |
| FR-11 | Privacy and notification controls | P0 |
| FR-12 | Simulated demo fallback | P0 |

## 9. Non-goals
- Money-based contests, betting, entry fees, or payouts.
- Scraping protected apps or reading external private data without authorized access.
- Using AI as the authority for scores, official events, or rankings.
- Guaranteeing instant reactions where OS or data provider timing prevents it.

## 10. Success measures
Track league setup completion, companion usage, user ability to understand rank changes, audited AI claim accuracy, snooze/disable rates, scoring reconciliation, duplicate-event rate, and data freshness. Establish numeric targets after initial usability tests.

## 11. Main risks
- External fantasy apps may not provide authorized roster/score APIs. Start with FanLens-managed rosters or permitted data imports.
- Data can be delayed. Show timestamp/status and a deterministic demo mode.
- Android overlay permissions/background limits exist. Make the overlay optional and provide widget/in-app fallbacks.
- AI may hallucinate. Keep scores deterministic and use validated template fallbacks.

## 12. MVP definition of done
A user creates or joins a private league, registers teams, triggers a simulated event, sees points and ranks update exactly once, switches between Always Reactive and Tap to Activate, and receives a grounded AI explanation only when allowed by their chosen mode. The experience remains functional without live data or AI.
