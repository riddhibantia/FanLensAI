from dataclasses import dataclass
from datetime import timedelta
from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai.prompt_builder import build_prompt, render_template
from app.ai.validator import InvalidInsightError, validate_llm_output
from app.models.insights import AIInsight
from app.models.matches import Match
from app.models.scoring import FantasyTeam, PointsLedger
from app.models.standings import StandingsSnapshot
from app.models.users import UserPreferences
from app.services.league_service import utcnow
from app.services.standings_service import StandingRow, compute_standings

# Default when the row has no stored preferences.
DEFAULT_PREFERENCES = {
    "companion_mode": "TAP_TO_ACTIVATE",
    "trigger_categories": [],
    "gap_threshold": 25,
    "ai_enabled": True,
}
INSIGHT_COOLDOWN = timedelta(minutes=5)

RANK_SWAP = "RANK_SWAP"
GAP_THRESHOLD = "GAP_THRESHOLD"
MATCH_END = "MATCH_END"
USER_QUESTION = "USER_QUESTION"


@dataclass(frozen=True)
class SnapshotRow:
    rank: int
    total_points: Decimal
    user_id: str
    gap_to_leader: Decimal


def get_or_default_preferences(session: Session, user_id: str) -> dict:
    prefs = session.get(UserPreferences, user_id)
    if prefs is None:
        return dict(DEFAULT_PREFERENCES)
    return {
        "companion_mode": prefs.companion_mode,
        "trigger_categories": prefs.trigger_categories or [],
        "gap_threshold": prefs.gap_threshold,
        "ai_enabled": prefs.ai_enabled,
    }


def _latest_snapshot(session: Session, match_id: str) -> dict[str, SnapshotRow]:
    batch_id = session.scalar(
        select(StandingsSnapshot.batch_id)
        .where(StandingsSnapshot.match_id == match_id)
        .order_by(StandingsSnapshot.captured_at.desc())
        .limit(1)
    )
    if batch_id is None:
        return {}
    rows = session.execute(
        select(
            StandingsSnapshot.fantasy_team_id,
            StandingsSnapshot.rank,
            StandingsSnapshot.total_points,
            StandingsSnapshot.user_id,
            StandingsSnapshot.gap_to_leader,
        ).where(StandingsSnapshot.batch_id == batch_id)
    )
    return {
        team_id: SnapshotRow(
            rank=rank,
            total_points=Decimal(str(total)),
            user_id=user_id,
            gap_to_leader=Decimal(str(gap)),
        )
        for team_id, rank, total, user_id, gap in rows
    }


def _contributing_events(
    session: Session, team_id: str, limit: int = 5
) -> list[dict]:
    rows = session.execute(
        select(PointsLedger.player_id, PointsLedger.points, PointsLedger.event_id)
        .where(PointsLedger.fantasy_team_id == team_id)
        .order_by(PointsLedger.created_at.desc())
        .limit(limit)
    )
    return [
        {"player_id": player_id, "points": str(points), "event_id": event_id}
        for player_id, points, event_id in rows
    ]


def detect_triggers(
    current: StandingRow,
    previous: SnapshotRow | None,
    prefs: dict,
    match: Match,
) -> list[str]:
    """Which proactive triggers this standings change should raise."""
    if previous is None:
        return []

    triggers: list[str] = []
    categories = set(prefs["trigger_categories"])

    if previous.rank != current.rank and RANK_SWAP in categories:
        triggers.append(RANK_SWAP)

    if GAP_THRESHOLD in categories:
        gap_change = abs(previous.gap_to_leader - current.gap_to_leader)
        if gap_change >= Decimal(str(prefs["gap_threshold"])):
            triggers.append(GAP_THRESHOLD)

    if match.status == "FINISHED" and MATCH_END in categories:
        triggers.append(MATCH_END)

    return triggers


def build_facts(
    session: Session,
    current: StandingRow,
    previous: SnapshotRow | None,
    rows: list[StandingRow],
) -> dict:
    leader = rows[0]
    contributing = _contributing_events(session, current.fantasy_team_id)
    event_ids = [entry["event_id"] for entry in contributing]
    facts = {
        "match_id": _match_id_of(session, current.fantasy_team_id),
        "league_id": current.league_id,
        "viewer_user_id": current.user_id,
        "team_name": current.team_name,
        "rank": current.rank,
        "previous_rank": previous.rank if previous else None,
        "total_points": current.total_points,
        "previous_total_points": previous.total_points if previous else None,
        "gap_to_leader": current.gap_to_leader,
        "previous_gap_to_leader": (
            current.gap_to_leader - current.delta_vs_previous if previous else None
        ),
        "leader_team_name": leader.team_name,
        "leader_total_points": leader.total_points,
        "contributing_events": contributing,
        "event_ids": event_ids,
        "participants": len(rows),
    }
    return facts


def _match_id_of(session: Session, fantasy_team_id: str) -> str:
    team = session.get(FantasyTeam, fantasy_team_id)
    return team.match_id if team else ""


def _recent_insight(
    session: Session, league_id: str, user_id: str, trigger_type: str
) -> AIInsight | None:
    cutoff = utcnow() - INSIGHT_COOLDOWN
    return session.scalar(
        select(AIInsight)
        .where(
            AIInsight.league_id == league_id,
            AIInsight.user_id == user_id,
            AIInsight.trigger_type == trigger_type,
            AIInsight.created_at >= cutoff,
        )
        .order_by(AIInsight.created_at.desc())
        .limit(1)
    )


def generate_insight(
    session: Session,
    league_id: str,
    user_id: str,
    match_id: str,
    trigger_type: str,
    facts: dict,
    llm_client=None,
    *,
    bypass_mode: bool = False,
) -> AIInsight | None:
    """Validates LLM output against server facts or falls back to a template."""
    prefs = get_or_default_preferences(session, user_id)
    if not prefs["ai_enabled"]:
        return None
    if not bypass_mode:
        if prefs["companion_mode"] != "ALWAYS_REACTIVE":
            return None
        if trigger_type not in set(prefs["trigger_categories"]):
            return None

    cached = _recent_insight(session, league_id, user_id, trigger_type)
    if cached is not None:
        return cached

    template_text = render_template(trigger_type, facts, facts.get("participants", 1))
    text = template_text
    source = "TEMPLATE"

    if llm_client is not None:
        try:
            raw = llm_client.explain(build_prompt(facts))
            text = validate_llm_output(raw, facts)
            source = "LLM"
        except Exception:
            # Bad JSON, invented number, timeout, quota: template is normal.
            text = template_text
            source = "TEMPLATE"

    insight = AIInsight(
        league_id=league_id,
        user_id=user_id,
        match_id=match_id,
        trigger_type=trigger_type,
        event_ids=facts["event_ids"],
        facts={k: str(v) for k, v in facts.items()},
        text=text,
        source=source,
        model=getattr(llm_client, "model", None) if source == "LLM" else None,
    )
    session.add(insight)
    session.commit()
    session.refresh(insight)
    return insight


def collect_proactive_insights(session: Session, match_id: str, llm_client=None) -> list[AIInsight]:
    """Detects triggers from the current standings vs the last snapshot and
    generates insights only for users whose preferences allow it."""
    rows = compute_standings(session, match_id)
    if not rows:
        return []

    match = session.get(Match, match_id)
    previous_snapshot = _latest_snapshot(session, match_id)

    insights: list[AIInsight] = []
    for row in rows:
        prefs = get_or_default_preferences(session, row.user_id)
        if not prefs["ai_enabled"]:
            continue
        if prefs["companion_mode"] != "ALWAYS_REACTIVE":
            continue
        triggers = detect_triggers(row, previous_snapshot.get(row.fantasy_team_id), prefs, match)
        if not triggers:
            continue
        facts = build_facts(session, row, previous_snapshot.get(row.fantasy_team_id), rows)
        for trigger_type in triggers:
            insight = generate_insight(
                session,
                row.league_id,
                row.user_id,
                match_id,
                trigger_type,
                facts,
                llm_client,
            )
            if insight is not None:
                insights.append(insight)
    return insights
