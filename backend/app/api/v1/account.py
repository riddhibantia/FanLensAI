from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_session
from app.models.users import User
from app.services.retention import delete_user_data

router = APIRouter()


@router.delete("/me")
def delete_account(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> dict[str, object]:
    """Delete the caller's account and private data (TRD §9)."""
    counts = delete_user_data(session, user.id)
    return {"user_id": user.id, "deleted": counts}
