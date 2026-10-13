from copy import deepcopy
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.matches import MatchEvent, PlayerMatchStats
from app.models.scoring import FantasyTeam, FantasyTeamPlayer, PointsLedger, ScoringRuleset
from app.services.event_adapter import find_existing_event, validate_payload
from app.services.scoring import engine


def _load_stats(session: Session, match_id: str, player_ids: list[str]) -> dict[str, dict]:
    stats: dict[str, dict] = {}
    for row in session.scalars(
        select(PlayerMatchStats).where(
            PlayerMatchStats.match_id == match_id,
            PlayerMatchStats.player_id.in_(player_ids),
        )
    ):
        stats[row.player_id] = row.stats or engine.empty_stats()
    for missing in set(player_ids) - set(stats):
        stats[missing] = engine.empty_stats()
    return stats


def _multiplier(is_captain: bool, is_vice: bool, multipliers: dict) -> Decimal:
    if is_captain:
        return Decimal(str(multipliers["captain"]))
    if is_vice:
        return Decimal(str(multipliers["vice"]))
    return Decimal(1)


def _persist_stats(session: Session, match_id: str, stats_after: dict[str, dict]) -> None:
    for player_id, stats in stats_after.items():
        existing = session.scalar(
            select(PlayerMatchStats).where(
                PlayerMatchStats.match_id == match_id,
                PlayerMatchStats.player_id == player_id,
            )
        )
        if existing is None:
            session.add(PlayerMatchStats(match_id=match_id, player_id=player_id, stats=stats))
        else:
            existing.stats = stats
        session.flush()


def process_match_event(session: Session, payload: dict) -> dict:
    """Scores one event through the TRD §5 flow. Idempotent per event.

    Returns a summary dict; ``status`` is "duplicate" when the event was seen
    before and produced no new ledger rows.
    """
    event_payload = validate_payload(payload)

    if (
        find_existing_event(
            session, event_payload["match_id"], event_payload["provider_event_id"]
        )
        is not None
    ):
        return {
            "status": "duplicate",
            "provider_event_id": event_payload["provider_event_id"],
        }

    match_id = event_payload["match_id"]
    event = MatchEvent(
        match_id=match_id,
        provider_event_id=event_payload["provider_event_id"],
        sequence=event_payload["sequence"],
        event_type=event_payload["event_type"],
        payload={k: v for k, v in event_payload.items() if k != "occurred_at"},
        occurred_at=event_payload["occurred_at"],
        status="ACCEPTED",
    )
    session.add(event)
    session.flush()

    players = [event_payload["batter"], event_payload["bowler"]]
    if event_payload["fielder"]:
        players.append(event_payload["fielder"])
    stats_before = _load_stats(session, match_id, players)
    stats_after = engine.apply_event_to_stats(event_payload, stats_before)

    teams = list(
        session.scalars(select(FantasyTeam).where(FantasyTeam.match_id == match_id))
    )
    if not teams:
        _persist_stats(session, match_id, stats_after)
        session.commit()
        return {"status": "scored", "event_id": event.id, "ledger_rows": 0}

    rosters: dict[str, dict[str, tuple[bool, bool]]] = {}
    for team_id, player_id, is_captain, is_vice in session.execute(
        select(
            FantasyTeamPlayer.fantasy_team_id,
            FantasyTeamPlayer.player_id,
            FantasyTeamPlayer.is_captain,
            FantasyTeamPlayer.is_vice,
        ).where(FantasyTeamPlayer.fantasy_team_id.in_([t.id for t in teams]))
    ):
        rosters.setdefault(team_id, {})[player_id] = (is_captain, is_vice)

    rulesets = {
        ruleset.id: ruleset
        for ruleset in session.scalars(
            select(ScoringRuleset).where(
                ScoringRuleset.id.in_({t.ruleset_id for t in teams})
            )
        )
    }

    awarded_flags: dict[str, dict] = {}
    deltas_out: list[dict] = []
    ledger_rows = 0

    for team in teams:
        ruleset = rulesets[team.ruleset_id]
        roster = rosters.get(team.id, {})
        # A fresh copy per ruleset keeps one-time bonus flags independent.
        team_stats_after = deepcopy(stats_after)
        deltas = engine.score_event(event_payload, ruleset.rules, stats_before, team_stats_after)

        for player_id, team_stats in team_stats_after.items():
            for flag, value in team_stats["bonuses_awarded"].items():
                if value:
                    awarded_flags.setdefault(player_id, {})[flag] = True

        # One ledger row per (event, team, player, ruleset_version): reasons for
        # the same player in one event are summed into that single row.
        totals: dict[str, list] = {}
        for delta in deltas:
            entry = totals.setdefault(delta.player_id, [0.0, []])
            entry[0] += delta.points
            entry[1].append(delta.reason)
        deltas_out = [
            {"player_id": player_id, "points": points, "reasons": reasons}
            for player_id, (points, reasons) in totals.items()
        ]

        for player_id, (total_points, _reasons) in totals.items():
            entry = roster.get(player_id)
            if entry is None:
                continue
            is_captain, is_vice = entry
            points = Decimal(str(total_points)) * _multiplier(
                is_captain, is_vice, ruleset.rules["multipliers"]
            )
            already = session.scalar(
                select(PointsLedger).where(
                    PointsLedger.event_id == event.id,
                    PointsLedger.fantasy_team_id == team.id,
                    PointsLedger.player_id == player_id,
                    PointsLedger.ruleset_version == ruleset.version,
                )
            )
            if already is not None:
                continue
            session.add(
                PointsLedger(
                    event_id=event.id,
                    fantasy_team_id=team.id,
                    player_id=player_id,
                    ruleset_version=ruleset.version,
                    points=points,
                )
            )
            ledger_rows += 1

    for player_id, flags in awarded_flags.items():
        stats_after[player_id]["bonuses_awarded"].update(flags)

    _persist_stats(session, match_id, stats_after)
    session.commit()
    return {
        "status": "scored",
        "event_id": event.id,
        "ledger_rows": ledger_rows,
        "deltas": deltas_out,
    }
