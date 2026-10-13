from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class LeagueCreateRequest(BaseModel):
    name: str = Field(min_length=1, max_length=80)


class LeagueResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    owner_user_id: str
    created_at: datetime


class LeagueCreatedResponse(LeagueResponse):
    invite_code: str
    invite_expires_at: datetime


class LeagueJoinRequest(BaseModel):
    invite_code: str = Field(min_length=1)


class MemberResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: str
    display_name: str
    role: str


class LeagueDetailResponse(LeagueResponse):
    members: list[MemberResponse]


class LeagueSummaryResponse(LeagueResponse):
    """One row in the user's circle list. Carries counts, never the invite code."""

    is_owner: bool
    member_count: int
    team_count: int


class MyLeaguesResponse(BaseModel):
    leagues: list[LeagueSummaryResponse]
