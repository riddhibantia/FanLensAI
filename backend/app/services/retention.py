"""Retention and deletion (TRD §9).

The prototype keeps every score ledger row (the audit trail) but purges
derived/cached rows — old AI insights and standings snapshots — after
``retention_days``. Account deletion removes a user's private data and
revokes their device tokens.
"""

from datetime import timedelta

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.models.insights import AIInsight
from app.models.leagues import League, LeagueMember
from app.models.scoring import FantasyTeam, FantasyTeamPlayer, PointsLedger
from app.models.standings import StandingsSnapshot
from app.models.users import DeviceToken, User, UserPreferences
from app.services.league_service import utcnow


def purge_stale_derived_rows(session: Session, older_than_days: int) -> dict[str, int]:
    """Delete insights/snapshots older than the cutoff. Returns counts."""
    cutoff = utcnow() - timedelta(days=older_than_days)
    insights = session.execute(
        delete(AIInsight).where(AIInsight.created_at < cutoff)
    ).rowcount or 0
    snapshots = session.execute(
        delete(StandingsSnapshot).where(StandingsSnapshot.captured_at < cutoff)
    ).rowcount or 0
    session.commit()
    return {"ai_insights": insights, "standings_snapshots": snapshots}


def delete_user_data(session: Session, user_id: str) -> dict[str, int]:
    """Remove a user's private data; revoke tokens; delete empty owned leagues."""
    counts: dict[str, int] = {}

    team_ids = list(
        session.scalars(select(FantasyTeam.id).where(FantasyTeam.user_id == user_id))
    )
    if team_ids:
        counts["fantasy_team_players"] = (
            session.execute(
                delete(FantasyTeamPlayer).where(
                    FantasyTeamPlayer.fantasy_team_id.in_(team_ids)
                )
            ).rowcount
            or 0
        )
        counts["points_ledger"] = (
            session.execute(
                delete(PointsLedger).where(PointsLedger.fantasy_team_id.in_(team_ids))
            ).rowcount
            or 0
        )
        counts["standings_snapshots"] = (
            session.execute(
                delete(StandingsSnapshot).where(
                    StandingsSnapshot.fantasy_team_id.in_(team_ids)
                )
            ).rowcount
            or 0
        )
        counts["fantasy_teams"] = (
            session.execute(
                delete(FantasyTeam).where(FantasyTeam.id.in_(team_ids))
            ).rowcount
            or 0
        )
    else:
        counts.update(
            {
                "fantasy_team_players": 0,
                "points_ledger": 0,
                "standings_snapshots": 0,
                "fantasy_teams": 0,
            }
        )

    counts["ai_insights"] = (
        session.execute(delete(AIInsight).where(AIInsight.user_id == user_id)).rowcount
        or 0
    )
    counts["league_members"] = (
        session.execute(
            delete(LeagueMember).where(LeagueMember.user_id == user_id)
        ).rowcount
        or 0
    )
    counts["user_preferences"] = (
        session.execute(
            delete(UserPreferences).where(UserPreferences.user_id == user_id)
        ).rowcount
        or 0
    )

    tokens = session.scalars(
        select(DeviceToken).where(
            DeviceToken.user_id == user_id, DeviceToken.revoked_at.is_(None)
        )
    ).all()
    now = utcnow()
    for token in tokens:
        token.revoked_at = now
    counts["device_tokens_revoked"] = len(tokens)

    # Delete leagues this user owned only when nobody else remains in them.
    owned_ids = list(
        session.scalars(select(League.id).where(League.owner_user_id == user_id))
    )
    deleted_leagues = 0
    for league_id in owned_ids:
        remaining = session.scalar(
            select(LeagueMember.id).where(LeagueMember.league_id == league_id).limit(1)
        )
        if remaining is None:
            session.execute(delete(League).where(League.id == league_id))
            deleted_leagues += 1
    counts["leagues_deleted"] = deleted_leagues

    user = session.get(User, user_id)
    if user is not None:
        session.delete(user)
        counts["users"] = 1
    else:
        counts["users"] = 0

    session.commit()
    return counts
