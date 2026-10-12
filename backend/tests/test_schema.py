import pytest
from sqlalchemy import inspect
from sqlalchemy.exc import IntegrityError

from tests.conftest import TRD_TABLES

from datetime import datetime

OCCURRED_AT = datetime(2026, 10, 10, 10, 0, 0)


def test_all_tables_exist(session) -> None:
    tables = set(inspect(session.get_bind()).get_table_names())
    assert TRD_TABLES <= tables


def test_unique_constraints_present(session) -> None:
    inspector = inspect(session.get_bind())

    def constraint_names(table: str) -> set[str]:
        return {c["name"] for c in inspector.get_unique_constraints(table)}

    assert "uq_ledger_idem" in constraint_names("points_ledger")
    assert "uq_league_user" in constraint_names("league_members")
    assert "uq_ruleset_name_version" in constraint_names("scoring_rulesets")


def test_duplicate_league_membership_rejected(session) -> None:
    from app.models.leagues import League, LeagueMember
    from app.models.users import User

    owner = User(display_name="Nikki")
    session.add(owner)
    session.flush()

    league = League(
        name="Sunday Smashers", owner_user_id=owner.id, invite_code_hash="hash-abc"
    )
    session.add(league)
    session.flush()

    session.add(LeagueMember(league_id=league.id, user_id=owner.id, role="OWNER"))
    session.flush()
    with pytest.raises(IntegrityError):
        session.add(LeagueMember(league_id=league.id, user_id=owner.id, role="MEMBER"))
        session.flush()


def test_duplicate_provider_event_id_rejected(session) -> None:
    from app.models.matches import Match, MatchEvent

    match = Match(title="IND vs AUS")
    session.add(match)
    session.flush()
    payload = {"ball": 1, "runs": 4}
    session.add(
        MatchEvent(
            match_id=match.id,
            provider_event_id="demo:1",
            sequence=1,
            event_type="RUNS",
            payload=payload,
            occurred_at=OCCURRED_AT,
        )
    )
    session.flush()
    with pytest.raises(IntegrityError):
        session.add(
            MatchEvent(
                match_id=match.id,
                provider_event_id="demo:1",
                sequence=1,
                event_type="RUNS",
                payload=payload,
                occurred_at=OCCURRED_AT,
            )
        )
        session.flush()


def test_duplicate_ledger_row_rejected(session) -> None:
    from app.models.leagues import League
    from app.models.matches import Match, MatchEvent
    from app.models.scoring import FantasyTeam, PointsLedger, ScoringRuleset
    from app.models.users import User

    owner = User(display_name="Nikki")
    session.add(owner)
    session.flush()
    league = League(
        name="Sunday Smashers", owner_user_id=owner.id, invite_code_hash="hash-abc"
    )
    match = Match(title="IND vs AUS")
    session.add_all([league, match])
    session.flush()
    ruleset = ScoringRuleset(name="standard", version="1.0", rules={})
    session.add(ruleset)
    session.flush()
    team = FantasyTeam(
        league_id=league.id,
        user_id=owner.id,
        match_id=match.id,
        name="Nikki XI",
        ruleset_id=ruleset.id,
    )
    session.add(team)
    session.flush()
    event = MatchEvent(
        match_id=match.id,
        provider_event_id="demo:5",
        sequence=5,
        event_type="RUNS",
        payload={"ball": 5, "runs": 1},
        occurred_at=OCCURRED_AT.replace(minute=5),
    )
    session.add(event)
    session.flush()

    def ledger_row() -> PointsLedger:
        return PointsLedger(
            event_id=event.id,
            fantasy_team_id=team.id,
            player_id="player-1",
            ruleset_version="1.0",
            points=5.0,
        )

    session.add(ledger_row())
    session.flush()
    with pytest.raises(IntegrityError):
        session.add(ledger_row())
        session.flush()


def test_duplicate_ruleset_name_version_rejected(session) -> None:
    from app.models.scoring import ScoringRuleset

    session.add(ScoringRuleset(name="standard", version="1.0", rules={}))
    session.flush()
    with pytest.raises(IntegrityError):
        session.add(ScoringRuleset(name="standard", version="1.0", rules={}))
        session.flush()
