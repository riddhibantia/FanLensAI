from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.matches import Match
from app.models.scoring import (
    FantasyTeam,
    FantasyTeamPlayer,
    ScoringRuleset,
)
from app.models.users import User
from app.schemas.rosters import (
    ROLE_MIN_MAX,
    ROSTER_SIZE,
    RosterCreateRequest,
    RosterPlayerIn,
)
from app.services.league_service import require_member


def validate_roster(players: list[RosterPlayerIn]) -> None:
    if len(players) != ROSTER_SIZE:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            f"roster must have exactly {ROSTER_SIZE} players",
        )
    ids = [p.player_id for p in players]
    if len(set(ids)) != len(ids):
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            "a player can appear only once in a roster",
        )
    for role, (lo, hi) in ROLE_MIN_MAX.items():
        count = sum(1 for p in players if p.role == role)
        if not lo <= count <= hi:
            raise HTTPException(
                status.HTTP_422_UNPROCESSABLE_ENTITY,
                f"{role} count must be between {lo} and {hi} (got {count})",
            )


def upsert_roster(
    session: Session, user: User, league_id: str, req: RosterCreateRequest
) -> FantasyTeam:
    require_member(session, league_id, user.id)

    match = session.get(Match, req.match_id)
    if match is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "match not found")
    ruleset = session.get(ScoringRuleset, req.ruleset_id)
    if ruleset is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "ruleset not found")

    validate_roster(req.players)

    team = session.scalar(
        select(FantasyTeam).where(
            FantasyTeam.match_id == req.match_id,
            FantasyTeam.user_id == user.id,
        )
    )
    if team is None:
        team = FantasyTeam(
            league_id=league_id,
            user_id=user.id,
            match_id=req.match_id,
            name=req.team_name,
            ruleset_id=ruleset.id,
        )
        session.add(team)
        session.flush()
    else:
        team.name = req.team_name
        team.ruleset_id = ruleset.id
        for existing in session.scalars(
            select(FantasyTeamPlayer).where(
                FantasyTeamPlayer.fantasy_team_id == team.id
            )
        ):
            session.delete(existing)
        session.flush()

    for p in req.players:
        session.add(
            FantasyTeamPlayer(
                fantasy_team_id=team.id,
                player_id=p.player_id,
                role=p.role,
                is_captain=p.is_captain,
                is_vice=p.is_vice,
            )
        )
    session.flush()
    return team
