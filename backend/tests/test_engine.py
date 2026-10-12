import json
from pathlib import Path

from app.services.scoring import engine

RULES = json.loads(
    Path(__file__).resolve().parents[1].joinpath("app", "data", "default_ruleset.json").read_text()
)


def _event(**overrides: object) -> dict:
    payload = {
        "batter": "p1",
        "bowler": "p2",
        "fielder": None,
        "runs": 0,
        "wicket": False,
        "wicket_kind": None,
    }
    payload.update(overrides)
    return payload


def _stats(runs: int = 0, balls: int = 0, wickets: int = 0) -> dict:
    stats = engine.empty_stats()
    stats["batting"]["runs"] = runs
    stats["batting"]["balls"] = balls
    stats["bowling"]["wickets"] = wickets
    return stats


def _run(payload: dict, before: dict[str, dict]) -> list[engine.Delta]:
    after = engine.apply_event_to_stats(payload, before)
    return engine.score_event(payload, RULES, before, after)


def test_boundary_and_six_points() -> None:
    four = _event(runs=4)
    reasons = {d.reason: d.points for d in _run(four, {"p1": _stats(), "p2": _stats()})}
    assert reasons == {"run": 4, "boundary": 1}

    six = _event(runs=6)
    reasons = {d.reason: d.points for d in _run(six, {"p1": _stats(), "p2": _stats()})}
    assert reasons == {"run": 6, "six": 2}


def test_single_run_only_run_points() -> None:
    deltas = _run(_event(runs=1), {"p1": _stats(), "p2": _stats()})
    assert {d.reason: d.points for d in deltas} == {"run": 1}


def test_milestone_fires_on_crossing_fifty() -> None:
    payload = _event(runs=1)
    deltas = _run(payload, {"p1": _stats(runs=49, balls=5), "p2": _stats()})
    assert {d.reason: d.points for d in deltas} == {"run": 1, "milestone_50": 8}


def test_no_milestone_at_forty_nine() -> None:
    payload = _event(runs=1)
    deltas = _run(payload, {"p1": _stats(runs=48, balls=5), "p2": _stats()})
    assert "milestone_50" not in {d.reason for d in deltas}


def test_duck_penalty_on_zero_run_wicket() -> None:
    payload = _event(wicket=True, wicket_kind="BOWLED")
    deltas = _run(payload, {"p1": _stats(), "p2": _stats()})
    reasons = {d.reason: d.points for d in deltas}
    assert reasons == {"duck": -5, "wicket_bowled": 25}


def test_caught_credits_bowler_and_fielder() -> None:
    payload = _event(wicket=True, wicket_kind="CAUGHT", fielder="p3")
    deltas = _run(payload, {"p1": _stats(runs=10), "p2": _stats(), "p3": _stats()})
    reasons = {d.reason: d.points for d in deltas}
    assert reasons["wicket_caught"] == 25
    assert reasons["catch"] == 8


def test_stumped_credits_keeper_not_bowler() -> None:
    payload = _event(wicket=True, wicket_kind="STUMPED", fielder="p3", runs=2)
    deltas = _run(payload, {"p1": _stats(runs=5), "p2": _stats(), "p3": _stats()})
    reasons = {d.reason: d.points for d in deltas}
    assert reasons == {"run": 2, "wicket_stumped": 12}


def test_haul_fires_on_fifth_wicket() -> None:
    payload = _event(wicket=True, wicket_kind="BOWLED", runs=4)
    deltas = _run(payload, {"p1": _stats(), "p2": _stats(wickets=4)})
    reasons = {d.reason: d.points for d in deltas}
    assert reasons["haul_5"] == 16


def test_strike_rate_bonus_needs_min_balls() -> None:
    too_few = _event(runs=6)
    deltas = _run(too_few, {"p1": _stats(balls=3), "p2": _stats()})
    assert "strike_rate" not in {d.reason for d in deltas}

    eligible = _event(runs=6)
    deltas = _run(eligible, {"p1": _stats(runs=13, balls=9), "p2": _stats()})
    assert {d.reason: d.points for d in deltas} == {
        "run": 6,
        "six": 2,
        "strike_rate": 5,
    }


def test_apply_event_to_stats_updates_counts() -> None:
    after = engine.apply_event_to_stats(
        _event(runs=4), {"p1": _stats(), "p2": _stats()}
    )
    assert after["p1"]["batting"] == {
        "runs": 4,
        "balls": 1,
        "fours": 1,
        "sixes": 0,
        "out": False,
    }
    assert after["p2"]["bowling"]["balls"] == 1
    assert after["p2"]["bowling"]["runs_conceded"] == 4
