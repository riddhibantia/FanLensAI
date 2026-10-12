from fastapi import Depends, Header, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.rate_limit import check
from app.core.security import hash_secret
from app.db.session import get_session
from app.models.leagues import LeagueMember
from app.models.scoring import FantasyTeam
from app.models.users import DeviceToken, User


def get_current_user(
    authorization: str | None = Header(default=None),
    session: Session = Depends(get_session),
) -> User:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "missing bearer token")
    raw = authorization.removeprefix("Bearer ")
    token = session.scalar(
        select(DeviceToken).where(
            DeviceToken.token_hash == hash_secret(raw),
            DeviceToken.revoked_at.is_(None),
        )
    )
    if token is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "invalid token")
    user = session.get(User, token.user_id)
    if user is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "user not found")
    return user


def require_match_access(session: Session, match_id: str, user_id: str) -> None:
    """A match is private: the user must belong to a league with a team in it."""
    is_member = session.scalar(
        select(LeagueMember.id)
        .join(FantasyTeam, FantasyTeam.league_id == LeagueMember.league_id)
        .where(
            FantasyTeam.match_id == match_id,
            LeagueMember.user_id == user_id,
        )
        .limit(1)
    )
    if is_member is None:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "no access to this match")


def rate_limit(request: Request) -> None:
    """Per-host, per-path limit for sensitive endpoints (join, explain)."""
    host = request.client.host if request.client else "unknown"
    check(host, request.url.path)


rate_limit_dependency = Depends(rate_limit)
