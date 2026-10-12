from pydantic import BaseModel, Field, field_validator

COMPANION_MODES = {"ALWAYS_REACTIVE", "TAP_TO_ACTIVATE"}
REFRESH_POLICIES = {"AUTO", "ON_OPEN", "MANUAL"}
MOTION_LEVELS = {"FULL", "CALM", "OFF"}
TRIGGER_CATEGORIES = {"RANK_SWAP", "GAP_THRESHOLD", "FOLLOWED_PLAYER", "MATCH_END"}
NOTIFICATION_CATEGORIES = {"INSIGHTS", "SCORES", "MATCH_EVENTS"}
TONES = {"NEUTRAL", "BANTER", "FORMAL"}


def _validate_in(choices: set[str], field: str):
    def validator(value: str | None) -> str | None:
        if value is not None and value not in choices:
            raise ValueError(f"{field} must be one of {sorted(choices)}")
        return value

    return validator


class PreferencesUpdate(BaseModel):
    """PATCH semantics: only fields present in the request are applied."""

    companion_mode: str | None = None
    overlay_enabled: bool | None = None
    refresh_policy: str | None = None
    trigger_categories: list[str] | None = None
    gap_threshold: int | None = Field(default=None, ge=0, le=500)
    quiet_hours: dict | None = None
    motion_level: str | None = None
    widget_fields: list[str] | None = None
    notification_categories: list[str] | None = None
    ai_enabled: bool | None = None
    tone: str | None = None

    _companion = field_validator("companion_mode")(_validate_in(COMPANION_MODES, "companion_mode"))
    _refresh = field_validator("refresh_policy")(_validate_in(REFRESH_POLICIES, "refresh_policy"))
    _motion = field_validator("motion_level")(_validate_in(MOTION_LEVELS, "motion_level"))
    _tone = field_validator("tone")(_validate_in(TONES, "tone"))

    @field_validator("trigger_categories")
    @classmethod
    def check_triggers(cls, value: list[str] | None) -> list[str] | None:
        if value is not None and not set(value) <= TRIGGER_CATEGORIES:
            raise ValueError(f"trigger_categories must be a subset of {sorted(TRIGGER_CATEGORIES)}")
        return value

    @field_validator("notification_categories")
    @classmethod
    def check_notifications(cls, value: list[str] | None) -> list[str] | None:
        if value is not None and not set(value) <= NOTIFICATION_CATEGORIES:
            raise ValueError(
                f"notification_categories must be a subset of {sorted(NOTIFICATION_CATEGORIES)}"
            )
        return value


class PreferencesResponse(BaseModel):
    user_id: str
    companion_mode: str
    overlay_enabled: bool
    refresh_policy: str
    trigger_categories: list[str]
    gap_threshold: int
    quiet_hours: dict
    motion_level: str
    widget_fields: list[str]
    notification_categories: list[str]
    ai_enabled: bool
    tone: str
