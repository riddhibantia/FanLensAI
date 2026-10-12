from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, new_id


class League(Base, TimestampMixin):
    __tablename__ = "leagues"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    name: Mapped[str] = mapped_column(String(80))
    owner_user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    invite_code_hash: Mapped[str] = mapped_column(String(64), unique=True)
    invite_expires_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class LeagueMember(Base, TimestampMixin):
    __tablename__ = "league_members"
    __table_args__ = (UniqueConstraint("league_id", "user_id", name="uq_league_user"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    league_id: Mapped[str] = mapped_column(ForeignKey("leagues.id"))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    role: Mapped[str] = mapped_column(String(10), default="MEMBER")  # OWNER | MEMBER
    joined_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
