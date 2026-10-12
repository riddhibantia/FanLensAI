from sqlalchemy import (
    Boolean,
    ForeignKey,
    Index,
    JSON,
    Numeric,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import Base, TimestampMixin, new_id


class ScoringRuleset(Base, TimestampMixin):
    __tablename__ = "scoring_rulesets"
    __table_args__ = (UniqueConstraint("name", "version", name="uq_ruleset_name_version"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    name: Mapped[str] = mapped_column(String(40))
    version: Mapped[str] = mapped_column(String(20))
    rules: Mapped[dict] = mapped_column(JSON)


class FantasyTeam(Base, TimestampMixin):
    __tablename__ = "fantasy_teams"
    __table_args__ = (UniqueConstraint("match_id", "user_id", name="uq_match_user_team"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    league_id: Mapped[str] = mapped_column(ForeignKey("leagues.id"))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    match_id: Mapped[str] = mapped_column(ForeignKey("matches.id"))
    name: Mapped[str] = mapped_column(String(80))
    ruleset_id: Mapped[str] = mapped_column(ForeignKey("scoring_rulesets.id"))
    players: Mapped[list["FantasyTeamPlayer"]] = relationship(back_populates="team")


class FantasyTeamPlayer(Base, TimestampMixin):
    __tablename__ = "fantasy_team_players"
    __table_args__ = (UniqueConstraint("fantasy_team_id", "player_id", name="uq_team_player"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    fantasy_team_id: Mapped[str] = mapped_column(ForeignKey("fantasy_teams.id"))
    player_id: Mapped[str] = mapped_column(String(64))
    role: Mapped[str] = mapped_column(String(20))  # BAT | BOWL | AR | WK
    is_captain: Mapped[bool] = mapped_column(Boolean, default=False)
    is_vice: Mapped[bool] = mapped_column(Boolean, default=False)
    team: Mapped["FantasyTeam"] = relationship(back_populates="players")


class PointsLedger(Base, TimestampMixin):
    __tablename__ = "points_ledger"
    __table_args__ = (
        # Idempotency key from TRD §6: one delta row per event/team/player/ruleset.
        UniqueConstraint(
            "event_id",
            "fantasy_team_id",
            "player_id",
            "ruleset_version",
            name="uq_ledger_idem",
        ),
        Index("ix_ledger_team", "fantasy_team_id"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    event_id: Mapped[str] = mapped_column(ForeignKey("match_events.id"))
    fantasy_team_id: Mapped[str] = mapped_column(ForeignKey("fantasy_teams.id"))
    player_id: Mapped[str] = mapped_column(String(64))
    ruleset_version: Mapped[str] = mapped_column(String(20))
    points: Mapped[float] = mapped_column(Numeric(12, 2))
