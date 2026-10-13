import json

# Questions the app offers as one-tap prompts, mapped to an intent. Matching is
# keyword-based and deliberately dumb: if nothing matches we fall back to the
# standings summary, which is still true, just less specific.
_INTENTS: list[tuple[str, tuple[str, ...]]] = [
    (
        "WHY_RANK",
        ("rank", "catching", "catch up", "overtake", "position", "standings change"),
    ),
    ("GAP", ("gap", "behind", "ahead", "lead", "margin", "close")),
    ("TOP_PICKS", ("pick", "player", "scored most", "best", "top", "who scored")),
    ("LAST_EVENTS", ("recent", "last", "event", "summar", "latest", "just happened")),
    (
        "SCENARIO",
        ("scenario", "what if", "if the next over", "if the next ball", "hypothetical"),
    ),
]


def _fmt(value) -> str:
    """47.00 -> "47", 47.50 -> "47.5". Keeps a clean number in prose."""
    number = float(value)
    if number == int(number):
        return str(int(number))
    return f"{number:g}"


def _intent_for(question: str | None) -> str | None:
    if not question:
        return None
    lowered = question.lower()
    for intent, needles in _INTENTS:
        if any(needle in lowered for needle in needles):
            return intent
    return None


def build_prompt(facts: dict) -> str:
    return (
        "You explain fantasy-cricket standings changes to one user.\n"
        "Use only the numbers in the facts; never invent a number or an event.\n"
        'Return strict JSON: {"text": "<one or two short sentences, max 280 chars>"}\n'
        f"FACTS: {json.dumps(facts, default=str)}"
    )


def render_template(trigger_type: str, facts: dict, participant_count: int) -> str:
    """Deterministic answers built only from server-computed facts.

    Every number here comes from the standings or the points ledger, so a
    template answer is always safe to show. It is a normal state, not an error.
    """
    leader = facts["leader_team_name"]
    total = _fmt(facts["total_points"])
    rank = facts["rank"]
    gap = _fmt(facts["gap_to_leader"])
    leader_total = _fmt(facts["leader_total_points"])

    # Explicit proactive triggers keep their dedicated wording.
    if trigger_type == "RANK_SWAP":
        return (
            f"You moved from rank {facts['previous_rank']} to {rank} "
            f"with {total} pts. {leader} leads with {leader_total}."
        )
    if trigger_type == "MATCH_END":
        return (
            f"Match over. You finished rank {rank} of {participant_count} "
            f"with {total} pts."
        )

    # A user question: answer the one they actually asked where we can.
    intent = _intent_for(facts.get("question"))
    if intent == "GAP":
        if rank == 1:
            return (
                f"You lead with {total} pts. The next team is "
                f"{_fmt(facts.get('second_gap', 0))} pts behind you."
            )
        return f"You're {gap} pts behind {leader} ({leader_total} pts)."
    if intent == "WHY_RANK":
        if facts.get("previous_rank") is not None and facts["previous_rank"] != rank:
            return (
                f"You moved from rank {facts['previous_rank']} to {rank}. "
                f"You now have {total} pts, {gap} behind {leader}."
            )
        return (
            f"Your rank is {rank} of {participant_count} with {total} pts, "
            f"{gap} pts behind {leader}. No rank change has been recorded yet."
        )
    if intent == "TOP_PICKS":
        picks = facts.get("top_picks") or []
        if not picks:
            return (
                f"No points have been credited to your picks yet. "
                f"You're rank {rank} with {total} pts."
            )
        listed = ", ".join(
            f"{p['player_id']} {_fmt(p['points'])} pts" for p in picks[:3]
        )
        return f"Your top scorers so far: {listed}. Team total {total} pts (rank {rank})."
    if intent == "LAST_EVENTS":
        events = facts.get("recent_events") or []
        if not events:
            return f"No events have been scored for your team yet. You have {total} pts."
        plural = "event" if len(events) == 1 else "events"
        return (
            f"I have {len(events)} recent scoring {plural} for your team. "
            f"You're rank {rank} with {total} pts."
        )
    if intent == "SCENARIO":
        # The server does not simulate matches, so say what is true instead.
        return (
            f"I can't predict a hypothetical over, but here's where you stand: "
            f"rank {rank} of {participant_count}, {total} pts, {gap} behind {leader}."
        )

    if trigger_type == "GAP_THRESHOLD":
        return f"You're now {gap} pts behind {leader} at {total} pts (rank {rank})."

    return (
        f"You have {total} pts (rank {rank} of {participant_count}), "
        f"{gap} pts behind {leader}."
    )
