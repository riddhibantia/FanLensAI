from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import generate_invite_code, hash_secret
from app.models.leagues import League, LeagueMember
from app.models.users import User


def utcnow() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def create_league(session: Session, owner: User, name: str) -> tuple[League, str]:
    """Creates the league and the owner's membership; returns (league, raw invite code)."""
    raw_code, code_hash = generate_invite_code()
    expires_at = utcnow() + timedelta(hours=get_settings().invite_ttl_hours)
    league = League(
        name=name,
        owner_user_id=owner.id,
        invite_code_hash=code_hash,
        invite_expires_at=expires_at,
    )
    session.add(league)
    session.flush()
    session.add(LeagueMember(league_id=league.id, user_id=owner.id, role="OWNER"))
    session.flush()
    return league, raw_code


def join_league(
    session: Session, user: User, league_id: str, invite_code: str
) -> LeagueMember:
    league = session.get(League, league_id)
    if league is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "league not found")
    if league.invite_expires_at is not None and league.invite_expires_at < utcnow():
        raise HTTPException(status.HTTP_410_GONE, "invite code expired")
    if hash_secret(invite_code) != league.invite_code_hash:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "invalid invite code")

    existing = session.scalar(
        select(LeagueMember).where(
            LeagueMember.league_id == league.id,
            LeagueMember.user_id == user.id,
        )
    )
    if existing is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, "already a member")

    member = LeagueMember(league_id=league.id, user_id=user.id, role="MEMBER")
    session.add(member)
    session.flush()
    return member


def get_member(session: Session, league_id: str, user_id: str) -> LeagueMember | None:
    return session.scalar(
        select(LeagueMember).where(
            LeagueMember.league_id == league_id,
            LeagueMember.user_id == user_id,
        )
    )


def require_member(session: Session, league_id: str, user_id: str) -> LeagueMember:
    member = get_member(session, league_id, user_id)
    if member is None:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "not a member of this league")
    return member
