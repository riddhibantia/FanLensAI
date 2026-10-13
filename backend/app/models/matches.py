from datetime import datetime

from sqlalchemy import JSON, DateTime, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, new_id


class Match(Base, TimestampMixin):
    __tablename__ = "matches"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    title: Mapped[str] = mapped_column(String(120))
    status: Mapped[str] = mapped_column(String(20), default="SCHEDULED")
    source: Mapped[str] = mapped_column(String(10), default="DEMO")  # DEMO | PROVIDER
    freshness: Mapped[str] = mapped_column(String(10), default="DEMO")  # LIVE|DELAYED|STALE|DEMO
    source_updated_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    scheduled_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class MatchEvent(Base, TimestampMixin):
    __tablename__ = "match_events"
    # Idempotency is per match: providers reuse event IDs across feeds, and two
    # leagues can watch the same fixture. A global unique key would let one
    # match's duplicate discard another match's genuine event.
    __table_args__ = (
        UniqueConstraint("match_id", "provider_event_id", name="uq_match_provider_event"),
    )

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    match_id: Mapped[str] = mapped_column(ForeignKey("matches.id"))
    provider_event_id: Mapped[str] = mapped_column(String(120))
    sequence: Mapped[int]
    event_type: Mapped[str] = mapped_column(String(40))
    payload: Mapped[dict] = mapped_column(JSON)
    occurred_at: Mapped[datetime] = mapped_column(DateTime)
    # ACCEPTED | CORRECTED | REJECTED; superseded_by points at the correcting event
    status: Mapped[str] = mapped_column(String(10), default="ACCEPTED")
    superseded_by: Mapped[str | None] = mapped_column(
        ForeignKey("match_events.id"), nullable=True
    )


class PlayerMatchStats(Base, TimestampMixin):
    __tablename__ = "player_match_stats"
    __table_args__ = (UniqueConstraint("match_id", "player_id", name="uq_match_player_stats"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    match_id: Mapped[str] = mapped_column(ForeignKey("matches.id"))
    # No players table in the TRD data model; player IDs come from the match/provider.
    player_id: Mapped[str] = mapped_column(String(64))
    stats: Mapped[dict] = mapped_column(JSON)
