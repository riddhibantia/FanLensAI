"""The deterministic fallback must answer the question that was asked.

A template reply is a normal state, not an error, so it has to be useful on its
own. Every number below comes from server facts; these tests pin down that the
wording changes with the question instead of repeating one generic sentence.
"""

from app.ai.prompt_builder import render_template


def _facts(**overrides) -> dict:
    facts = {
        "team_name": "Nikki XI",
        "rank": 2,
        "previous_rank": 2,
        "total_points": "47.00",
        "gap_to_leader": "6.50",
        "leader_team_name": "Rahul XI",
        "leader_total_points": "53.50",
        "second_gap": "6.50",
        "top_picks": [
            {"player_id": "p1", "points": "18.00"},
            {"player_id": "p4", "points": "12.50"},
        ],
        "recent_events": [
            {"player_id": "p1", "points": "18.00", "event_id": "e1"},
        ],
    }
    facts.update(overrides)
    return facts


def _answer(question: str, **overrides) -> str:
    return render_template("USER_QUESTION", _facts(question=question, **overrides), 2)


def test_decimal_points_render_without_trailing_zeros():
    assert "47 pts" in _answer("how are you?")
    assert "47.00" not in _answer("how are you?")


def test_gap_question_reports_the_real_gap():
    text = _answer("how big is the gap to the leader?")
    assert "6.5" in text
    assert "Rahul XI" in text


def test_gap_question_from_the_leader_names_the_second_team():
    text = _answer("how big is the gap to the leader?", rank=1, gap_to_leader="0")
    assert "lead" in text.lower()


def test_top_picks_question_lists_ledger_totals():
    text = _answer("which of my picks scored the most points?")
    assert "p1 18 pts" in text
    assert "p4 12.5 pts" in text


def test_top_picks_question_with_no_ledger_rows_says_so():
    text = _answer("which of my picks scored the most points?", top_picks=[])
    assert "No points have been credited" in text


def test_rank_question_admits_when_no_change_was_recorded():
    text = _answer("why did my rank change?")
    assert "No rank change has been recorded" in text


def test_rank_question_describes_a_real_swap():
    text = _answer("why did my rank change?", previous_rank=3)
    assert "from rank 3 to 2" in text


def test_scenario_question_refuses_to_predict():
    text = _answer("what if the next over goes for 10 runs?")
    assert "can't predict" in text
    # It must still hand back the true standing rather than nothing.
    assert "47 pts" in text


def test_recent_events_question_counts_real_events():
    text = _answer("summarise the most recent events")
    assert "1 recent scoring event for" in text


def test_unknown_question_falls_back_to_the_standings_summary():
    text = _answer("how are you?")
    assert "47 pts" in text
    assert "rank 2 of 2" in text


def test_questions_do_not_all_produce_the_same_answer():
    questions = [
        "why did my rank change?",
        "how big is the gap to the leader?",
        "which of my picks scored the most points?",
        "summarise the most recent events",
        "what if the next over goes for 10 runs?",
    ]
    answers = {_answer(q) for q in questions}
    assert len(answers) == len(questions)


def test_proactive_triggers_keep_their_own_wording():
    facts = _facts(previous_rank=3)
    text = render_template("RANK_SWAP", facts, 2)
    assert "from rank 3 to 2" in text
    assert "Rahul XI" in text

    ended = render_template("MATCH_END", facts, 2)
    assert "rank 2 of 2" in ended


def test_every_chip_question_in_the_app_maps_to_a_branch():
    """The one-tap prompts are only useful if each hits its own answer."""
    from app.ai.prompt_builder import _intent_for

    expected = {
        "Why did my rank change?": "WHY_RANK",
        "Why did the standings change?": "WHY_RANK",
        "How big is the gap to the leader?": "GAP",
        "Which of my picks scored the most points?": "TOP_PICKS",
        "Which of my picks scored most?": "TOP_PICKS",
        "Summarise the most recent events in my match.": "LAST_EVENTS",
    }
    for question, intent in expected.items():
        assert _intent_for(question) == intent, question


def test_a_plain_question_is_not_mistaken_for_a_scenario():
    from app.ai.prompt_builder import _intent_for

    assert _intent_for("how are you?") is None
    assert _intent_for("what is 47 pts in words?") is None


def test_a_second_question_is_not_answered_with_the_first_answer():
    """USER_QUESTION must never hit the proactive cooldown cache.

    The cooldown is keyed on trigger_type; every typed question shares the
    USER_QUESTION type, so caching them made the second question return the
    first one's text for five minutes.
    """
    import inspect

    from app.services import insight_service

    source = inspect.getsource(insight_service.generate_insight)
    assert "trigger_type != USER_QUESTION" in source
    assert "_recent_insight(session, league_id, user_id, trigger_type)" in source
