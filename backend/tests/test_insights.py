import json

import pytest

from app.services.insight_service import (
    GAP_THRESHOLD,
    RANK_SWAP,
    collect_proactive_insights,
    generate_insight,
    get_or_default_preferences,
)
from app.services.scoring.pipeline import process_match_event
from app.services.standings_service import recalculate_and_snapshot


class FakeLLM:
    def __init__(self, response: str, model: str = "fake-1"):
        self.response = response
        self.model = model

    def explain(self, prompt: str) -> str:
        return self.response


class BrokenLLM:
    model = "broken"

    def explain(self, prompt: str) -> str:
        raise TimeoutError("quota exceeded")


def _facts(seeded, rows, **overrides) -> dict:
    leader = rows[0]
    row = rows[0]
    facts = {
        "match_id": seeded["match_id"],
        "league_id": seeded.get("league_id", "league-1"),
        "viewer_user_id": row.user_id,
        "team_name": row.team_name,
        "rank": row.rank,
        "previous_rank": 2,
        "total_points": row.total_points,
        "previous_total_points": 10,
        "gap_to_leader": row.gap_to_leader,
        "previous_gap_to_leader": 5,
        "leader_team_name": leader.team_name,
        "leader_total_points": leader.total_points,
        "contributing_events": [],
        "event_ids": [],
        "participants": len(rows),
    }
    facts.update(overrides)
    return facts


def _league_id(session, seeded) -> str:
    from app.models.scoring import FantasyTeam

    return session.get(FantasyTeam, seeded["team_id"]).league_id


def _set_prefs(session, seeded, **values) -> None:
    from app.models.users import UserPreferences

    prefs = session.get(UserPreferences, seeded["user_id"])
    if prefs is None:
        prefs = UserPreferences(user_id=seeded["user_id"])
        session.add(prefs)
    for key, value in values.items():
        setattr(prefs, key, value)
    session.commit()


def _reactive_prefs(session, seeded) -> None:
    _set_prefs(
        session,
        seeded,
        companion_mode="ALWAYS_REACTIVE",
        trigger_categories=["RANK_SWAP", "GAP_THRESHOLD", "MATCH_END"],
        ai_enabled=True,
    )


def test_valid_llm_output_is_accepted(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)
    llm = FakeLLM(json.dumps({"text": f"You have {rows[0].total_points} pts, rank 1."}))

    insight = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        llm,
        bypass_mode=True,
    )
    assert insight is not None
    assert insight.source == "LLM"
    assert "rank 1" in insight.text


def test_invented_number_falls_back_to_template(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)
    llm = FakeLLM(json.dumps({"text": "You have 999999 pts and lead everyone."}))

    insight = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        llm,
        bypass_mode=True,
    )
    assert insight is not None
    assert insight.source == "TEMPLATE"
    assert "999999" not in insight.text


def test_malformed_json_falls_back_to_template(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)
    llm = FakeLLM("not json at all")

    insight = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        llm,
        bypass_mode=True,
    )
    assert insight.source == "TEMPLATE"


def test_llm_error_falls_back_to_template(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)

    insight = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        BrokenLLM(),
        bypass_mode=True,
    )
    assert insight.source == "TEMPLATE"


def test_unknown_event_id_falls_back(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows, event_ids=["demo:0001"])
    llm = FakeLLM(
        json.dumps({"text": "Event demo:9999 gave you 10 pts."})
    )

    insight = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        llm,
        bypass_mode=True,
    )
    assert insight.source == "TEMPLATE"


def test_tap_to_activate_suppresses_proactive(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    _set_prefs(session, seeded, companion_mode="TAP_TO_ACTIVATE")
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)

    assert (
        generate_insight(
            session, league_id, seeded["user_id"], seeded["match_id"], RANK_SWAP, facts, None
        )
        is None
    )
    # A direct question is still allowed in tap-only mode.
    assert (
        generate_insight(
            session,
            league_id,
            seeded["user_id"],
            seeded["match_id"],
            "USER_QUESTION",
            facts,
            None,
            bypass_mode=True,
        )
        is not None
    )


def test_ai_disabled_blocks_even_direct_questions(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    _set_prefs(session, seeded, ai_enabled=False)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)

    assert (
        generate_insight(
            session,
            league_id,
            seeded["user_id"],
            seeded["match_id"],
            "USER_QUESTION",
            facts,
            None,
            bypass_mode=True,
        )
        is None
    )


def test_cooldown_returns_cached_insight(session, seeded) -> None:
    league_id = _league_id(session, seeded)
    rows, _ = recalculate_and_snapshot(session, seeded["match_id"])
    facts = _facts(seeded, rows)

    first = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        None,
        bypass_mode=True,
    )
    second = generate_insight(
        session,
        league_id,
        seeded["user_id"],
        seeded["match_id"],
        RANK_SWAP,
        facts,
        None,
        bypass_mode=True,
    )
    assert first.id == second.id


def _add_rival_team(session, seeded) -> None:
    """A second roster so ranks can actually swap."""
    from app.models.leagues import League, LeagueMember
    from app.models.scoring import FantasyTeam, FantasyTeamPlayer
    from app.models.users import User

    league_id = _league_id(session, seeded)
    rival_user = User(display_name="Rahul")
    session.add(rival_user)
    session.flush()
    session.add(
        LeagueMember(league_id=league_id, user_id=rival_user.id, role="MEMBER")
    )
    rival = FantasyTeam(
        league_id=league_id,
        user_id=rival_user.id,
        match_id=seeded["match_id"],
        name="Rahul XI",
        ruleset_id=seeded["ruleset_id"],
    )
    session.add(rival)
    session.flush()
    session.add(
        FantasyTeamPlayer(
            fantasy_team_id=rival.id, player_id="p4", role="BAT", is_captain=True, is_vice=False
        )
    )
    session.commit()


def _event(sequence: int, batter: str, runs: int, **overrides) -> dict:
    payload = {
        "match_id": "",
        "provider_event_id": f"demo:{sequence:04d}",
        "sequence": sequence,
        "occurred_at": "2026-10-10T10:00:00",
        "batter": batter,
        "bowler": "p2",
        "runs": runs,
    }
    payload.update(overrides)
    return payload


def test_rank_swap_insight_only_in_reactive_mode(session, seeded) -> None:
    _add_rival_team(session, seeded)
    _set_prefs(
        session,
        seeded,
        companion_mode="ALWAYS_REACTIVE",
        trigger_categories=["RANK_SWAP"],
        ai_enabled=True,
    )

    # Baseline: rival takes the lead with a six; Nikki is rank 2.
    process_match_event(session, _event(1, "p4", 6, match_id=seeded["match_id"]))
    recalculate_and_snapshot(session, seeded["match_id"])
    from app.services.standings_service import compute_standings

    rows = compute_standings(session, seeded["match_id"])
    rank_of_nikki = next(r.rank for r in rows if r.user_id == seeded["user_id"])
    assert rank_of_nikki == 2

    # Nikki overtakes the rival: rank 2 -> 1 must raise RANK_SWAP once.
    process_match_event(session, _event(2, "p1", 4, match_id=seeded["match_id"]))
    process_match_event(session, _event(3, "p1", 4, match_id=seeded["match_id"]))
    insights = collect_proactive_insights(session, seeded["match_id"])
    assert [i.trigger_type for i in insights] == ["RANK_SWAP"]
    assert insights[0].source == "TEMPLATE"

    # Switching to TAP_TO_ACTIVATE suppresses further proactive insights.
    _set_prefs(session, seeded, companion_mode="TAP_TO_ACTIVATE")
    process_match_event(
        session, _event(4, "p1", 6, match_id=seeded["match_id"])
    )
    assert collect_proactive_insights(session, seeded["match_id"]) == []


def test_default_preferences_are_conservative(session, seeded) -> None:
    from app.models.users import UserPreferences

    user_id = "user-without-prefs"
    prefs = get_or_default_preferences(session, user_id)
    assert prefs["companion_mode"] == "TAP_TO_ACTIVATE"
    assert prefs["trigger_categories"] == []
