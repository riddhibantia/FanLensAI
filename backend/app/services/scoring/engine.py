from dataclasses import dataclass


@dataclass(frozen=True)
class Delta:
    player_id: str
    points: float
    reason: str


WICKET_BOWLER_CREDIT = {"BOWLED", "CAUGHT", "LBW"}


def empty_stats() -> dict:
    return {
        "batting": {"runs": 0, "balls": 0, "fours": 0, "sixes": 0, "out": False},
        "bowling": {"balls": 0, "runs_conceded": 0, "wickets": 0},
        "fielding": {"catches": 0, "stumpings": 0, "run_outs": 0},
        "bonuses_awarded": {"strike_rate": False, "economy": False},
    }


def _stats_for(stats_before: dict[str, dict], player_id: str | None) -> dict | None:
    if not player_id:
        return None
    existing = stats_before.get(player_id)
    if existing is None:
        return empty_stats()
    return {
        "batting": dict(existing["batting"]),
        "bowling": dict(existing["bowling"]),
        "fielding": dict(existing["fielding"]),
        "bonuses_awarded": dict(existing["bonuses_awarded"]),
    }


def apply_event_to_stats(payload: dict, stats_before: dict[str, dict]) -> dict[str, dict]:
    """Updates cumulative stats for everyone involved in the event. Rules-free."""
    runs = int(payload.get("runs", 0) or 0)
    wicket = bool(payload.get("wicket"))
    kind = payload.get("wicket_kind")
    batter, bowler, fielder = payload.get("batter"), payload.get("bowler"), payload.get("fielder")

    stats_after: dict[str, dict] = {}

    bat = _stats_for(stats_before, batter)
    if bat is not None:
        b = bat["batting"]
        b["runs"] += runs
        b["balls"] += 1
        if runs == 4:
            b["fours"] += 1
        elif runs == 6:
            b["sixes"] += 1
        if wicket:
            b["out"] = True
        stats_after[batter] = bat

    bowl = _stats_for(stats_before, bowler)
    if bowl is not None:
        bo = bowl["bowling"]
        bo["balls"] += 1
        bo["runs_conceded"] += runs
        if wicket and kind in WICKET_BOWLER_CREDIT:
            bo["wickets"] += 1
        stats_after[bowler] = bowl

    fld = _stats_for(stats_before, fielder)
    if fld is not None and wicket:
        f = fld["fielding"]
        if kind == "CAUGHT":
            f["catches"] += 1
        elif kind == "STUMPED":
            f["stumpings"] += 1
        elif kind == "RUN_OUT":
            f["run_outs"] += 1
        stats_after[fielder] = fld

    return stats_after


def score_event(
    payload: dict,
    rules: dict,
    stats_before: dict[str, dict],
    stats_after: dict[str, dict],
) -> list[Delta]:
    """Deterministically turns one event plus its stats context into point deltas.

    Milestone and rate bonuses fire only when a threshold is crossed by this
    event, and one-time flags in the stats prevent a second award on replay of
    a different event.
    """
    runs = int(payload.get("runs", 0) or 0)
    wicket = bool(payload.get("wicket"))
    kind = payload.get("wicket_kind")
    batter, bowler, fielder = payload.get("batter"), payload.get("bowler"), payload.get("fielder")

    bat_rules = rules["batting"]
    bow_rules = rules["bowling"]
    field_rules = rules["fielding"]

    deltas: list[Delta] = []

    if batter:
        before = _stats_for(stats_before, batter)
        after = stats_after[batter]
        old_bat, new_bat = before["batting"], after["batting"]

        if runs:
            deltas.append(Delta(batter, runs * bat_rules["run"], "run"))
        if runs == 4:
            deltas.append(Delta(batter, bat_rules["boundary_bonus"], "boundary"))
        elif runs == 6:
            deltas.append(Delta(batter, bat_rules["six_bonus"], "six"))

        if old_bat["runs"] < 100 <= new_bat["runs"]:
            deltas.append(Delta(batter, bat_rules["milestone_100"], "milestone_100"))
        elif old_bat["runs"] < 50 <= new_bat["runs"]:
            deltas.append(Delta(batter, bat_rules["milestone_50"], "milestone_50"))

        if wicket and new_bat["runs"] == 0:
            deltas.append(Delta(batter, bat_rules["duck"], "duck"))

        bonuses = after["bonuses_awarded"]
        if not bonuses["strike_rate"] and new_bat["balls"] >= bat_rules["min_balls_for_bonus"]:
            strike_rate = new_bat["runs"] / new_bat["balls"] * 100
            if strike_rate >= bat_rules["strike_rate_threshold"]:
                bonuses["strike_rate"] = True
                deltas.append(Delta(batter, bat_rules["strike_rate_bonus"], "strike_rate"))

    if wicket:
        credit_player = None
        points = 0.0
        reason = f"wicket_{str(kind).lower()}"
        if kind in WICKET_BOWLER_CREDIT:
            credit_player, points = bowler, bow_rules["wicket"]
        elif kind == "STUMPED":
            credit_player, points = fielder, bow_rules["stumping"]
        elif kind == "RUN_OUT":
            credit_player, points = fielder, field_rules["run_out"]
        if credit_player:
            deltas.append(Delta(credit_player, points, reason))
        if kind == "CAUGHT" and fielder:
            deltas.append(Delta(fielder, field_rules["catch"], "catch"))

    if bowler:
        before = _stats_for(stats_before, bowler)
        after = stats_after[bowler]
        old_wickets, new_wickets = before["bowling"]["wickets"], after["bowling"]["wickets"]

        if old_wickets < 5 <= new_wickets:
            deltas.append(Delta(bowler, bow_rules["haul_5"], "haul_5"))

        bonuses = after["bonuses_awarded"]
        new_balls = after["bowling"]["balls"]
        if (
            not bonuses["economy"]
            and new_balls >= bow_rules["min_balls_for_bonus"]
        ):
            economy = after["bowling"]["runs_conceded"] / new_balls * 6
            if economy < bow_rules["economy_threshold"]:
                bonuses["economy"] = True
                deltas.append(Delta(bowler, bow_rules["economy_bonus"], "economy"))

    return deltas
