# FanLens AI — backend

FastAPI + SQLAlchemy + Alembic. The backend computes every score, rank, gap and
delta; the LLM only explains numbers this server already produced.

## Local setup

```powershell
python -m venv .venv
.\.venv\Scripts\pip install -e ".[dev]"
Copy-Item .env.example .env   # set FANLENS_DATABASE_URL and FANLENS_AI_API_KEY
```

SQLite works for local dev (`sqlite:///./dev.db`). Point `FANLENS_DATABASE_URL`
at a managed PostgreSQL URL when deploying.

## Migrations and seed

```powershell
.\.venv\Scripts\python.exe -m alembic upgrade head
.\.venv\Scripts\python.exe scripts\seed_demo.py   # prints user tokens + invite code
```

## Run

```powershell
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload
```

`GET /healthz` returns status without any secrets.

## Tests

```powershell
.\.venv\Scripts\python.exe -m pytest -q
```

## API surface (TRD §7)

| Method | Path |
|---|---|
| POST | `/v1/auth/device` |
| POST | `/v1/leagues` |
| POST | `/v1/leagues/{id}/join` |
| GET | `/v1/leagues/{id}` |
| POST | `/v1/leagues/{id}/rosters` |
| GET | `/v1/leagues/{id}/standings?match_id=` |
| GET | `/v1/matches/{id}/events?after=` |
| POST | `/v1/matches/{id}/events` |
| POST | `/v1/demo/matches/{id}/events` |
| GET | `/v1/leagues/{id}/insights` |
| POST | `/v1/leagues/{id}/insights/explain` |
| PATCH | `/v1/me/preferences` |
| DELETE | `/v1/me` (account + private data deletion, revokes tokens) |

Auth: `Authorization: Bearer <token>` from `/v1/auth/device`.

## Retention (TRD §9)

Derived rows (`ai_insights`, `standings_snapshots`) older than
`FANLENS_RETENTION_DAYS` (default 90) are purged by:

```powershell
.\.venv\Scripts\python.exe scripts\purge_stale.py
```

Run it on a schedule (cron / managed scheduler) in production. Score ledger
rows are never purged — they are the audit trail.

## Layout

```
app/
  api/v1/       HTTP routes (leagues, rosters, standings, events, demo, insights, preferences)
  ai/           LLM client, prompt builder, output validator, fallback templates
  core/         config, logging, security helpers, rate limiting
  data/         ruleset v1 and demo ball-by-ball script (JSON)
  models/       SQLAlchemy models (one file per TRD table group)
  schemas/      Pydantic request/response models
  services/     event adapter, scoring engine + pipeline, standings, insights
alembic/        migrations
scripts/        seed_demo.py
tests/          pytest suite
```

## AI rules that protect scores

- `app/ai/validator.py` rejects any LLM output containing a number that does not
  match server facts, or an unknown event ID.
- Failed validation → `app/ai/prompt_builder.py` template. Template fallback is a
  normal state.
- No API key (demo) means insight generation always uses templates.
