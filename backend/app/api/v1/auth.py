from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_session
from app.core.security import generate_device_token
from app.models.users import DeviceToken, User
from app.schemas.auth import DeviceAuthRequest, DeviceAuthResponse

router = APIRouter()


@router.post("/auth/device", response_model=DeviceAuthResponse)
def device_auth(
    body: DeviceAuthRequest, session: Session = Depends(get_session)
) -> DeviceAuthResponse:
    user = User(display_name=body.display_name)
    session.add(user)
    session.flush()
    raw_token, token_hash = generate_device_token()
    session.add(DeviceToken(user_id=user.id, token_hash=token_hash))
    session.flush()
    session.commit()
    return DeviceAuthResponse(user_id=user.id, token=raw_token)
