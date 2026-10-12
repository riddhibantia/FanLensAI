from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_session
from app.models.users import User
from app.schemas.rosters import RosterCreateRequest, RosterResponse
from app.services.roster_service import upsert_roster

router = APIRouter()


@router.post(
    "/leagues/{league_id}/rosters",
    response_model=RosterResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_or_update_roster(
    league_id: str,
    body: RosterCreateRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> RosterResponse:
    team = upsert_roster(session, user, league_id, body)
    session.commit()
    session.refresh(team)
    return RosterResponse(
        id=team.id,
        match_id=team.match_id,
        team_name=team.name,
        ruleset_id=team.ruleset_id,
        players=[
            {
                "player_id": p.player_id,
                "role": p.role,
                "is_captain": p.is_captain,
                "is_vice": p.is_vice,
            }
            for p in team.players
        ],
    )
