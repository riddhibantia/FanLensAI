from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Numeric, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, new_id


class StandingsSnapshot(Base):
    __tablename__ = "standings_snapshots"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    league_id: Mapped[str] = mapped_column(ForeignKey("leagues.id"))
    match_id: Mapped[str] = mapped_column(ForeignKey("matches.id"))
    fantasy_team_id: Mapped[str] = mapped_column(ForeignKey("fantasy_teams.id"))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    # Groups the rows of one full standings recalculation.
    batch_id: Mapped[str] = mapped_column(String(36))
    total_points: Mapped[float] = mapped_column(Numeric(12, 2))
    rank: Mapped[int]
    gap_to_leader: Mapped[float] = mapped_column(Numeric(12, 2))
    delta_vs_previous: Mapped[float] = mapped_column(Numeric(12, 2))
    captured_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
