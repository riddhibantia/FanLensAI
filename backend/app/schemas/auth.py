from pydantic import BaseModel, Field


class DeviceAuthRequest(BaseModel):
    display_name: str = Field(min_length=1, max_length=80)
    device_id: str = Field(min_length=1, max_length=64)


class DeviceAuthResponse(BaseModel):
    user_id: str
    token: str


class MeResponse(BaseModel):
    user_id: str
    display_name: str
