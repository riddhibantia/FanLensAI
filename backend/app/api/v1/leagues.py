from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_session, rate_limit_dependency
from app.models.leagues import League, LeagueMember
from app.models.users import User
from app.schemas.leagues import (
    LeagueCreateRequest,
    LeagueCreatedResponse,
    LeagueDetailResponse,
    LeagueJoinRequest,
)
from app.services.league_service import create_league, get_member, join_league

router = APIRouter()


@router.post(
    "/leagues", response_model=LeagueCreatedResponse, status_code=status.HTTP_201_CREATED
)
def create_league_route(
    body: LeagueCreateRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> LeagueCreatedResponse:
    league, raw_code = create_league(session, user, body.name)
    session.commit()
    return LeagueCreatedResponse(
        id=league.id,
        name=league.name,
        owner_user_id=league.owner_user_id,
        created_at=league.created_at,
        invite_code=raw_code,
        invite_expires_at=league.invite_expires_at,
    )


@router.post(
    "/leagues/{league_id}/join",
    response_model=None,
    status_code=status.HTTP_201_CREATED,
    dependencies=[rate_limit_dependency],
)
def join_league_route(
    league_id: str,
    body: LeagueJoinRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> dict[str, str]:
    join_league(session, user, league_id, body.invite_code)
    session.commit()
    return {"league_id": league_id, "user_id": user.id}


@router.get("/leagues/{league_id}", response_model=LeagueDetailResponse)
def get_league_route(
    league_id: str,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> LeagueDetailResponse:
    league = session.get(League, league_id)
    if league is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "league not found")
    if get_member(session, league_id, user.id) is None:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "not a member of this league")

    rows = session.execute(
        select(User.display_name, LeagueMember.role, LeagueMember.user_id)
        .join(LeagueMember, LeagueMember.user_id == User.id)
        .where(LeagueMember.league_id == league_id)
    ).all()
    members = [
        {"user_id": r.user_id, "display_name": r.display_name, "role": r.role}
        for r in rows
    ]
    return LeagueDetailResponse(
        id=league.id,
        name=league.name,
        owner_user_id=league.owner_user_id,
        created_at=league.created_at,
        members=members,
    )
