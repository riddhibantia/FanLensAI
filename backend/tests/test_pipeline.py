import json

import pytest
from sqlalchemy import select

from app.models.matches import MatchEvent, PlayerMatchStats
from app.models.scoring import PointsLedger
from app.services.event_adapter import InvalidEventError
from app.services.scoring.pipeline import process_match_event


def _payload(**overrides: object) -> dict:
    payload = {
        "match_id": "",
        "provider_event_id": "demo:0001",
        "sequence": 1,
        "occurred_at": "2026-10-10T10:00:00",
        "event_type": "BALL",
        "batter": "p1",
        "bowler": "p2",
        "fielder": None,
        "runs": 0,
        "wicket": False,
        "wicket_kind": None,
    }
    payload.update(overrides)
    return payload


def _rows(session, team_id: str) -> list[PointsLedger]:
    return list(
        session.scalars(
            select(PointsLedger).where(PointsLedger.fantasy_team_id == team_id)
        )
    )


def _total(session, team_id: str) -> float:
    return float(sum(r.points for r in _rows(session, team_id)))


def test_boundary_scores_with_captain_multiplier(session, seeded) -> None:
    result = process_match_event(
        session, _payload(match_id=seeded["match_id"], runs=4)
    )
    assert result["status"] == "scored"
    rows = _rows(session, seeded["team_id"])
    assert len(rows) == 1
    assert float(rows[0].points) == 10.0  # (4 + 1 boundary) x captain
    assert rows[0].ruleset_version == "1.0"


def test_replaying_same_event_is_noop(session, seeded) -> None:
    payload = _payload(match_id=seeded["match_id"], runs=4)
    first = process_match_event(session, payload)
    assert first["ledger_rows"] == 1

    second = process_match_event(session, payload)
    assert second["status"] == "duplicate"
    assert _total(session, seeded["team_id"]) == 10.0
    assert session.query(MatchEvent).count() == 1


def test_vice_captain_multiplier_on_bowler(session, seeded) -> None:
    process_match_event(
        session,
        _payload(
            match_id=seeded["match_id"],
            wicket=True,
            wicket_kind="BOWLED",
            provider_event_id="demo:0002",
            sequence=2,
        ),
    )
    rows = _rows(session, seeded["team_id"])
    by_player = {r.player_id: float(r.points) for r in rows}
    assert by_player["p2"] == 37.5  # 25 x vice
    assert by_player["p1"] == -10.0  # duck -5 x captain


def test_completed_event_updates_player_stats(session, seeded) -> None:
    process_match_event(
        session, _payload(match_id=seeded["match_id"], runs=6)
    )
    stats = session.scalar(
        select(PlayerMatchStats).where(
            PlayerMatchStats.match_id == seeded["match_id"],
            PlayerMatchStats.player_id == "p1",
        )
    )
    assert stats.stats["batting"] == {
        "runs": 6,
        "balls": 1,
        "fours": 0,
        "sixes": 1,
        "out": False,
    }


def test_milestone_fires_once_from_persisted_stats(session, seeded) -> None:
    session.add(
        PlayerMatchStats(
            match_id=seeded["match_id"],
            player_id="p1",
            stats={
                "batting": {"runs": 49, "balls": 40, "fours": 5, "sixes": 1, "out": False},
                "bowling": {"balls": 0, "runs_conceded": 0, "wickets": 0},
                "fielding": {"catches": 0, "stumpings": 0, "run_outs": 0},
                "bonuses_awarded": {"strike_rate": False, "economy": False},
            },
        )
    )
    session.commit()

    process_match_event(session, _payload(match_id=seeded["match_id"], runs=1))
    reason_rows = [
        r for r in _rows(session, seeded["team_id"]) if r.player_id == "p1"
    ]
    assert float(sum(r.points for r in reason_rows)) == 18.0  # (1 + 8) x captain


def test_invalid_event_raises(session, seeded) -> None:
    with pytest.raises(InvalidEventError):
        process_match_event(
            session, _payload(match_id=seeded["match_id"], wicket=True)
        )
    session.rollback()


def test_missing_fielder_on_caught_raises(session, seeded) -> None:
    with pytest.raises(InvalidEventError):
        process_match_event(
            session,
            _payload(match_id=seeded["match_id"], wicket=True, wicket_kind="CAUGHT"),
        )
    session.rollback()


def test_event_without_teams_still_records_stats(session, seeded) -> None:
    from app.models.matches import Match

    match = Match(title="IND vs SA", status="LIVE")
    session.add(match)
    session.commit()

    result = process_match_event(
        session, _payload(match_id=match.id, runs=2, provider_event_id="demo:0003")
    )
    assert result["status"] == "scored"
    assert result["ledger_rows"] == 0
    assert (
        session.query(MatchEvent)
        .filter(MatchEvent.provider_event_id == "demo:0003")
        .count()
        == 1
    )
