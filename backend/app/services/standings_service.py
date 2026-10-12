from dataclasses import dataclass
from datetime import datetime, timedelta
from decimal import Decimal

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.base import new_id
from app.models.matches import Match
from app.models.scoring import FantasyTeam, PointsLedger
from app.models.standings import StandingsSnapshot
from app.services.league_service import utcnow


@dataclass(frozen=True)
class StandingRow:
    league_id: str
    fantasy_team_id: str
    user_id: str
    team_name: str
    total_points: Decimal
    rank: int
    gap_to_leader: Decimal
    delta_vs_previous: Decimal


def match_freshness(match: Match, now: datetime | None = None) -> str:
    if match.source == "DEMO":
        return "DEMO"
    if match.source_updated_at is None:
        return "STALE"
    age = (now or utcnow()) - match.source_updated_at
    if age <= timedelta(seconds=30):
        return "LIVE"
    if age <= timedelta(minutes=5):
        return "DELAYED"
    return "STALE"


def _totals_by_team(session: Session, match_id: str) -> dict[str, Decimal]:
    teams = list(
        session.scalars(select(FantasyTeam).where(FantasyTeam.match_id == match_id))
    )
    totals = {team.id: Decimal("0") for team in teams}
    rows = session.execute(
        select(PointsLedger.fantasy_team_id, func.sum(PointsLedger.points))
        .join(FantasyTeam, FantasyTeam.id == PointsLedger.fantasy_team_id)
        .where(FantasyTeam.match_id == match_id)
        .group_by(PointsLedger.fantasy_team_id)
    )
    for team_id, total in rows:
        totals[team_id] = Decimal(str(total or 0))
    return totals


def _previous_totals(session: Session, match_id: str) -> dict[str, Decimal]:
    """Totals from the most recent snapshot batch, if any."""
    latest_batch = session.scalar(
        select(StandingsSnapshot.batch_id)
        .where(StandingsSnapshot.match_id == match_id)
        .order_by(StandingsSnapshot.captured_at.desc())
        .limit(1)
    )
    if latest_batch is None:
        return {}
    return {
        team_id: Decimal(str(total))
        for team_id, total in session.execute(
            select(StandingsSnapshot.fantasy_team_id, StandingsSnapshot.total_points).where(
                StandingsSnapshot.batch_id == latest_batch
            )
        )
    }


def compute_standings(session: Session, match_id: str) -> list[StandingRow]:
    teams = list(
        session.scalars(select(FantasyTeam).where(FantasyTeam.match_id == match_id))
    )
    if not teams:
        return []

    totals = _totals_by_team(session, match_id)
    previous = _previous_totals(session, match_id)

    # Deterministic order: points desc, then earliest roster, then name.
    ordered = sorted(teams, key=lambda t: (-totals[t.id], t.created_at, t.name))
    leader_total = totals[ordered[0].id]

    return [
        StandingRow(
            league_id=team.league_id,
            fantasy_team_id=team.id,
            user_id=team.user_id,
            team_name=team.name,
            total_points=totals[team.id],
            rank=position,
            gap_to_leader=leader_total - totals[team.id],
            delta_vs_previous=(
                totals[team.id] - previous[team.id]
                if team.id in previous
                else totals[team.id]
            ),
        )
        for position, team in enumerate(ordered, start=1)
    ]


def recalculate_and_snapshot(
    session: Session, match_id: str
) -> tuple[list[StandingRow], str]:
    """Computes standings and writes one snapshot batch. Returns (rows, batch_id)."""
    rows = compute_standings(session, match_id)
    batch_id = new_id()
    for row in rows:
        session.add(
            StandingsSnapshot(
                league_id=row.league_id,
                match_id=match_id,
                fantasy_team_id=row.fantasy_team_id,
                user_id=row.user_id,
                batch_id=batch_id,
                total_points=row.total_points,
                rank=row.rank,
                gap_to_leader=row.gap_to_leader,
                delta_vs_previous=row.delta_vs_previous,
            )
        )
    session.flush()
    return rows, batch_id
