from decimal import Decimal

from app.models.matches import Match
from app.services.scoring.pipeline import process_match_event
from app.services.standings_service import compute_standings, match_freshness, recalculate_and_snapshot


def _payload(**overrides) -> dict:
    payload = {
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


def test_single_team_stands_first(session, seeded) -> None:
    process_match_event(session, _payload(match_id=seeded["match_id"], runs=4))
    rows, batch_id = recalculate_and_snapshot(session, seeded["match_id"])
    assert len(rows) == 1
    row = rows[0]
    assert row.rank == 1
    assert row.total_points == Decimal("10.0")
    assert row.gap_to_leader == Decimal("0")
    assert row.delta_vs_previous == Decimal("10.0")
    assert batch_id


def test_replay_does_not_change_totals(session, seeded) -> None:
    payload = _payload(match_id=seeded["match_id"], runs=4)
    process_match_event(session, payload)
    first, _ = recalculate_and_snapshot(session, seeded["match_id"])
    process_match_event(session, payload)
    second, _ = recalculate_and_snapshot(session, seeded["match_id"])
    assert first[0].total_points == second[0].total_points == Decimal("10.0")
    assert second[0].delta_vs_previous == Decimal("0")


def test_tie_break_is_deterministic(session, seeded) -> None:
    from app.models.scoring import FantasyTeam

    other = FantasyTeam(
        league_id=session.get(FantasyTeam, seeded["team_id"]).league_id,
        user_id="user-2",
        match_id=seeded["match_id"],
        name="Rahul XI",
        ruleset_id=seeded["ruleset_id"],
    )
    session.add(other)
    session.commit()

    # Both teams have identical totals; the earlier-created team must rank first.
    rows = compute_standings(session, seeded["match_id"])
    assert [row.fantasy_team_id for row in rows] == [seeded["team_id"], other.id]
    assert rows[0].rank == 1 and rows[1].rank == 2
    assert rows[1].gap_to_leader == Decimal("0")


def test_rank_orders_by_points(session, seeded, rules) -> None:
    from app.models.leagues import League
    from app.models.matches import Match
    from app.models.scoring import FantasyTeam, FantasyTeamPlayer
    from app.models.users import User

    first = session.get(FantasyTeam, seeded["team_id"])
    other_user = User(display_name="Rahul")
    session.add(other_user)
    session.flush()
    league = session.get(League, first.league_id)
    rival = FantasyTeam(
        league_id=league.id,
        user_id=other_user.id,
        match_id=seeded["match_id"],
        name="Rahul XI",
        ruleset_id=seeded["ruleset_id"],
    )
    session.add(rival)
    session.flush()
    for player_id, role, captain, vice in [("p4", "BAT", True, False)]:
        session.add(
            FantasyTeamPlayer(
                fantasy_team_id=rival.id,
                player_id=player_id,
                role=role,
                is_captain=captain,
                is_vice=vice,
            )
        )
    session.commit()

    process_match_event(
        session, _payload(match_id=seeded["match_id"], runs=4)
    )
    process_match_event(
        session,
        _payload(
            match_id=seeded["match_id"],
            provider_event_id="demo:0002",
            sequence=2,
            batter="p4",
            runs=6,
        ),
    )
    rows = compute_standings(session, seeded["match_id"])
    by_user = {row.user_id: row for row in rows}
    assert by_user[other_user.id].rank == 1
    assert by_user[seeded["user_id"]].rank == 2
    leader = rows[0]
    trailer = rows[1]
    assert leader.gap_to_leader == Decimal("0")
    assert trailer.gap_to_leader == Decimal("6.0")  # (6+2)x2 - 10


def test_freshness_states(session, seeded) -> None:
    from datetime import datetime, timedelta

    match = session.get(Match, seeded["match_id"])
    assert match_freshness(match) == "DEMO"

    match.source = "PROVIDER"
    match.source_updated_at = datetime.utcnow() - timedelta(seconds=10)
    assert match_freshness(match) == "LIVE"

    match.source_updated_at = datetime.utcnow() - timedelta(minutes=2)
    assert match_freshness(match) == "DELAYED"

    match.source_updated_at = datetime.utcnow() - timedelta(hours=1)
    assert match_freshness(match) == "STALE"


def test_standings_empty_without_teams(session) -> None:
    from app.models.matches import Match

    match = Match(title="Empty", status="SCHEDULED")
    session.add(match)
    session.commit()
    assert compute_standings(session, match.id) == []
