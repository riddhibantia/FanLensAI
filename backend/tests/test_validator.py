import json

import pytest

from app.ai.validator import InvalidInsightError, validate_llm_output

FACTS = {
    "rank": 2,
    "previous_rank": 3,
    "total_points": 45.5,
    "previous_total_points": 12,
    "gap_to_leader": 18,
    "previous_gap_to_leader": 4,
    "gap_to_rank_ahead": 18,
    "leader_team_name": "Rahul XI",
    "leader_total_points": 63.5,
    "event_ids": ["demo:0001", "demo:0002"],
    "contributing_events": [
        {"player_id": "p1", "points": "10", "event_id": "demo:0001"},
        {"player_id": "p2", "points": "5", "event_id": "demo:0002"},
    ],
}


def test_accepts_text_using_only_fact_numbers() -> None:
    raw = json.dumps(
        {"text": "You have 45.5 pts (rank 2), 18 pts behind Rahul XI on 63.5 pts."}
    )
    assert validate_llm_output(raw, FACTS).startswith("You have 45.5")


def test_rejects_invented_number() -> None:
    raw = json.dumps({"text": "You gained 400 points from one ball!"})
    with pytest.raises(InvalidInsightError):
        validate_llm_output(raw, FACTS)


def test_rejects_non_json() -> None:
    with pytest.raises(InvalidInsightError):
        validate_llm_output("sorry, I cannot", FACTS)


def test_rejects_wrong_shape() -> None:
    with pytest.raises(InvalidInsightError):
        validate_llm_output(json.dumps({"answer": "..."}), FACTS)


def test_rejects_missing_text() -> None:
    with pytest.raises(InvalidInsightError):
        validate_llm_output(json.dumps({"text": None}), FACTS)


def test_rejects_too_long() -> None:
    with pytest.raises(InvalidInsightError):
        validate_llm_output(json.dumps({"text": "x" * 281}), FACTS)


def test_rejects_unknown_event_id() -> None:
    raw = json.dumps({"text": "Event demo:0007 moved you up to rank 2."})
    with pytest.raises(InvalidInsightError):
        validate_llm_output(raw, FACTS)


def test_accepts_known_event_id() -> None:
    raw = json.dumps({"text": "Event demo:0001 moved you up to rank 2."})
    assert validate_llm_output(raw, FACTS)
