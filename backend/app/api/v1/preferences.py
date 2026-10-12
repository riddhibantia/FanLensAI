from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_session
from app.models.users import User, UserPreferences
from app.schemas.preferences import PreferencesResponse, PreferencesUpdate

router = APIRouter()


@router.patch("/me/preferences", response_model=PreferencesResponse)
def update_preferences(
    body: PreferencesUpdate,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> PreferencesResponse:
    prefs = session.get(UserPreferences, user.id)
    if prefs is None:
        prefs = UserPreferences(user_id=user.id)
        session.add(prefs)
        session.flush()

    for field, value in body.model_dump(exclude_unset=True).items():
        # Explicit nulls mean "leave unchanged", never "erase to NULL".
        if value is not None:
            setattr(prefs, field, value)
    session.commit()
    session.refresh(prefs)
    return PreferencesResponse(
        user_id=prefs.user_id,
        companion_mode=prefs.companion_mode,
        overlay_enabled=prefs.overlay_enabled,
        refresh_policy=prefs.refresh_policy,
        trigger_categories=prefs.trigger_categories,
        gap_threshold=prefs.gap_threshold,
        quiet_hours=prefs.quiet_hours,
        motion_level=prefs.motion_level,
        widget_fields=prefs.widget_fields,
        notification_categories=prefs.notification_categories,
        ai_enabled=prefs.ai_enabled,
        tone=prefs.tone,
    )
