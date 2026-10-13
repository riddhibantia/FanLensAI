# FanLens AI — Demo runbook

How to get real data into the app so the floating score pet, the widget and the
leaderboard show actual numbers instead of an empty state.

Every number in the app comes from the backend's scoring engine. Nothing is
mocked on the device, so the app needs the backend running and seeded.

---

## 1. Start the backend

From `backend/`:

```bash
python -m venv .venv
.venv/Scripts/pip install -e ".[dev]"     # Windows
# source .venv/bin/activate && pip install -e ".[dev]"   # macOS/Linux

cp .env.example .env                       # then edit it
```

Set `FANLENS_DATABASE_URL` in `.env`. SQLite is fine for a local demo:

```
FANLENS_DATABASE_URL=sqlite:///./dev.db
```

`FANLENS_AI_API_KEY` is optional. Without it the AI explanation falls back to a
deterministic template, which is a normal state — the app still works, it just
won't have LLM-written banter.

```bash
.venv/Scripts/python -m alembic upgrade head
.venv/Scripts/python scripts/seed_demo.py
.venv/Scripts/python -m uvicorn app.main:app --reload
```

`seed_demo.py` prints three things you will need:

```
league_id: <uuid>   match_id: <uuid>
invite code (show once): <code>
token[Nikki]: <token>
token[Rahul]: <token>
```

It creates two users (Nikki, Rahul), a league, rosters for both, and a live
match titled **"IND vs AUS (demo)"**. Because that match's `source` is `DEMO`,
the API reports its freshness as `DEMO` — so every screen that shows its scores
is visibly labelled as simulated. That is intentional, not a bug.

The seed is safe to re-run; it skips anything that already exists.

## 2. Point the app at it

The debug build defaults to `http://10.0.2.2:8000/`, which is the host machine
as seen from an Android **emulator**. That works with no extra setup.

For a **physical device over USB**, create `android/local.properties`
(gitignored) with:

```
fanlens.baseUrl=http://127.0.0.1:8000/
```

then run:

```bash
adb reverse tcp:8000 tcp:8000
```

## 3. Populate the app

1. Install and open the app. Sign in with any display name — this registers a
   device token.
2. Join the seeded league using the invite code from step 1, and select the
   **IND vs AUS (demo)** match as the active match.
3. Scores appear. The freshness chip reads `DEMO`.

### To make scores move

Any of these pushes scripted balls through the real scoring engine:

- **Matches screen** → simulate button (`injectDemoBalls(1)`)
- **Circle hub dialog** → the demo action (`injectDemoBalls(5)`)
- **Floating pet** → tap it → **Load demo scores** (`injectDemoBalls(5)` plus a
  standings refresh)

Replaying the same demo balls does not double-count points — the ledger is
keyed per `(event_id, fantasy_team_id, player_id, ruleset_version)`.

## 4. Show the floating pet

1. Companion screen → enable the overlay. Android will ask for
   **"Display over other apps"** permission.
2. The pet appears over whatever app you switch to. Drag it anywhere; it
   remembers where you left it, including across a reboot.
3. Tap it to expand in place — rank, points, gap to leader, and who you are
   chasing. **Open App** leaves the overlay; the pet itself never opens the app
   on tap.

The pet only animates in `ALWAYS_REACTIVE` mode with motion enabled and outside
quiet hours. In `TAP_TO_ACTIVATE` it stays completely still. That is by design.

---

## If the pet says "No scores yet"

That is the honest empty state — there are no cached standings for the active
match. It means one of:

- the backend is not running, or the app cannot reach it (check the base URL)
- no match is selected as active (sign in, join the league, pick the match)
- the roster has not been registered, so there is nothing to score

Expand the pet and press **Load demo scores**. If no match is active yet, that
button opens the app instead, because the seed has to be selected first.

## Do not

- **Do not ship a build pointing at the demo backend with the demo seeded.**
  Demo and production credentials are separate (TRD §2).
- **Do not put the AI provider key in the app.** It lives on the server only.
  The key field in settings is a local developer override, not the shipped path.
