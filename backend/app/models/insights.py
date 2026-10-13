from sqlalchemy import JSON, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, new_id


class AIInsight(Base, TimestampMixin):
    __tablename__ = "ai_insights"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    league_id: Mapped[str] = mapped_column(ForeignKey("leagues.id"))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    match_id: Mapped[str] = mapped_column(ForeignKey("matches.id"))
    trigger_type: Mapped[str] = mapped_column(String(30))  # RANK_SWAP|GAP_THRESHOLD|...
    event_ids: Mapped[list] = mapped_column(JSON)
    facts: Mapped[dict] = mapped_column(JSON)
    text: Mapped[str] = mapped_column(Text)
    source: Mapped[str] = mapped_column(String(10))  # LLM | TEMPLATE
    model: Mapped[str | None] = mapped_column(String(40), nullable=True)
