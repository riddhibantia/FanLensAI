from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_session
from app.models.matches import Match
from app.models.scoring import FantasyTeam
from app.models.users import User
from app.schemas.standings import StandingsResponse
from app.services.league_service import require_member
from app.services.standings_service import match_freshness, recalculate_and_snapshot

router = APIRouter()


@router.get("/leagues/{league_id}/standings", response_model=StandingsResponse)
def get_standings(
    league_id: str,
    match_id: str | None = Query(default=None),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> StandingsResponse:
    require_member(session, league_id, user.id)

    if match_id is None:
        match_id = session.scalar(
            select(FantasyTeam.match_id)
            .where(FantasyTeam.league_id == league_id)
            .order_by(FantasyTeam.created_at.desc())
            .limit(1)
        )
    match = session.get(Match, match_id) if match_id else None
    if match is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "match not found")

    rows, _batch_id = recalculate_and_snapshot(session, match.id)
    session.commit()
    return StandingsResponse(
        match_id=match.id,
        freshness=match_freshness(match),
        freshness_updated_at=match.source_updated_at,
        rows=[
            {
                "rank": row.rank,
                "fantasy_team_id": row.fantasy_team_id,
                "user_id": row.user_id,
                "team_name": row.team_name,
                "total_points": row.total_points,
                "gap_to_leader": row.gap_to_leader,
                "delta_vs_previous": row.delta_vs_previous,
            }
            for row in rows
        ],
    )
