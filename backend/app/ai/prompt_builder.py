import json


def _fmt(value) -> str:
    return str(int(value)) if float(value) == int(float(value)) else str(value)


def build_prompt(facts: dict) -> str:
    return (
        "You explain fantasy-cricket standings changes to one user.\n"
        "Use only the numbers in the facts; never invent a number or an event.\n"
        'Return strict JSON: {"text": "<one or two short sentences, max 280 chars>"}\n'
        f"FACTS: {json.dumps(facts, default=str)}"
    )


def render_template(trigger_type: str, facts: dict, participant_count: int) -> str:
    leader = facts["leader_team_name"]
    total = _fmt(facts["total_points"])
    rank = facts["rank"]

    if trigger_type == "RANK_SWAP":
        return (
            f"You moved from rank {facts['previous_rank']} to {rank} "
            f"with {total} pts. {leader} leads with "
            f"{_fmt(facts['leader_total_points'])}."
        )
    if trigger_type == "GAP_THRESHOLD":
        return (
            f"You're now {_fmt(facts['gap_to_leader'])} pts behind {leader} "
            f"at {total} pts (rank {rank})."
        )
    if trigger_type == "MATCH_END":
        return (
            f"Match over. You finished rank {rank} of {participant_count} "
            f"with {total} pts."
        )
    return (
        f"You have {total} pts (rank {rank} of {participant_count}), "
        f"{_fmt(facts['gap_to_leader'])} pts behind {leader}."
    )
