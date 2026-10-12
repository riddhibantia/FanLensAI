from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, JSON, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base, TimestampMixin, new_id


class User(Base, TimestampMixin):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    display_name: Mapped[str] = mapped_column(String(80))
    email: Mapped[str | None] = mapped_column(String(255), nullable=True)


class DeviceToken(Base):
    __tablename__ = "device_tokens"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=new_id)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"))
    token_hash: Mapped[str] = mapped_column(String(64), unique=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)


class UserPreferences(Base):
    __tablename__ = "user_preferences"

    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), primary_key=True)
    companion_mode: Mapped[str] = mapped_column(String(20), default="TAP_TO_ACTIVATE")
    overlay_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    refresh_policy: Mapped[str] = mapped_column(String(10), default="ON_OPEN")
    trigger_categories: Mapped[list] = mapped_column(JSON, default=list)
    gap_threshold: Mapped[int] = mapped_column(Integer, default=25)
    quiet_hours: Mapped[dict] = mapped_column(JSON, default=dict)
    motion_level: Mapped[str] = mapped_column(String(10), default="CALM")
    widget_fields: Mapped[list] = mapped_column(JSON, default=list)
    notification_categories: Mapped[list] = mapped_column(JSON, default=list)
    ai_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    tone: Mapped[str] = mapped_column(String(20), default="NEUTRAL")
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now()
    )
