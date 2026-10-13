import json
from pathlib import Path

import pytest
from alembic.config import Config
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from alembic import command

ROOT = Path(__file__).resolve().parents[1]

TRD_TABLES = {
    "users",
    "leagues",
    "league_members",
    "matches",
    "scoring_rulesets",
    "fantasy_teams",
    "fantasy_team_players",
    "match_events",
    "player_match_stats",
    "points_ledger",
    "standings_snapshots",
    "user_preferences",
    "ai_insights",
    # device-token auth (not in TRD list; required by the chosen auth scheme)
    "device_tokens",
}


@pytest.fixture()
def session(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Session:
    db_file = tmp_path / "fanlens.db"
    monkeypatch.setenv("FANLENS_DATABASE_URL", f"sqlite:///{db_file.as_posix()}")

    cfg = Config(str(ROOT / "alembic.ini"))
    cfg.set_main_option("script_location", str(ROOT / "alembic"))
    command.upgrade(cfg, "head")

    engine = create_engine(f"sqlite:///{db_file.as_posix()}")
    session = Session(engine)
    try:
        yield session
    finally:
        session.close()
        engine.dispose()


@pytest.fixture()
def rules() -> dict:
    return json.loads(
        (ROOT / "app" / "data" / "default_ruleset.json").read_text(encoding="utf-8")
    )


@pytest.fixture()
def seeded(session: Session, rules: dict) -> dict:
    """League, match, one roster (p1 captain, p2 vice, p3 member)."""
    from app.models.leagues import League
    from app.models.matches import Match
    from app.models.scoring import FantasyTeam, FantasyTeamPlayer, ScoringRuleset
    from app.models.users import User

    user = User(display_name="Nikki")
    session.add(user)
    session.flush()  # assigns user.id so the league can reference it
    league = League(name="Sunday Smashers", owner_user_id=user.id, invite_code_hash="h")
    match = Match(title="IND vs AUS", status="LIVE")
    session.add_all([league, match])
    session.flush()

    ruleset = ScoringRuleset(
        name=rules["name"], version=rules["version"], rules=rules
    )
    session.add(ruleset)
    session.flush()
    team = FantasyTeam(
        league_id=league.id,
        user_id=user.id,
        match_id=match.id,
        name="Nikki XI",
        ruleset_id=ruleset.id,
    )
    session.add(team)
    session.flush()
    for player_id, role, captain, vice in [
        ("p1", "BAT", True, False),
        ("p2", "BOWL", False, True),
        ("p3", "WK", False, False),
    ]:
        session.add(
            FantasyTeamPlayer(
                fantasy_team_id=team.id,
                player_id=player_id,
                role=role,
                is_captain=captain,
                is_vice=vice,
            )
        )
    session.commit()
    return {
        "team_id": team.id,
        "match_id": match.id,
        "ruleset_id": ruleset.id,
        "user_id": user.id,
    }


@pytest.fixture()
def client(session: Session):
    from fastapi.testclient import TestClient

    from app.api.deps import get_session
    from app.main import app

    app.dependency_overrides[get_session] = lambda: session
    yield TestClient(app)
    app.dependency_overrides.clear()

