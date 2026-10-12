from pydantic import BaseModel, ConfigDict, Field, model_validator

ROSTER_SIZE = 11
ROLE_PATTERN = "^(BAT|BOWL|WK|AR)$"
# Captain/vice multipliers live in the scoring ruleset, not here.
ROLE_MIN_MAX = {"BAT": (1, 6), "BOWL": (3, 6), "WK": (1, 2), "AR": (1, 4)}


class RosterPlayerIn(BaseModel):
    player_id: str = Field(min_length=1, max_length=64)
    role: str = Field(pattern=ROLE_PATTERN)
    is_captain: bool = False
    is_vice: bool = False


class RosterCreateRequest(BaseModel):
    match_id: str
    team_name: str = Field(min_length=1, max_length=80)
    ruleset_id: str
    players: list[RosterPlayerIn] = Field(min_length=1)

    @model_validator(mode="after")
    def check_captains(self) -> "RosterCreateRequest":
        captains = [p for p in self.players if p.is_captain]
        vices = [p for p in self.players if p.is_vice]
        if len(captains) != 1:
            raise ValueError("exactly one captain is required")
        if len(vices) > 1:
            raise ValueError("at most one vice-captain is allowed")
        return self


class RosterPlayerResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    player_id: str
    role: str
    is_captain: bool
    is_vice: bool


class RosterResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: str
    match_id: str
    team_name: str
    ruleset_id: str
    players: list[RosterPlayerResponse]
