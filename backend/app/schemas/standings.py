from decimal import Decimal

from pydantic import BaseModel


class StandingRowResponse(BaseModel):
    rank: int
    fantasy_team_id: str
    user_id: str
    team_name: str
    total_points: Decimal
    gap_to_leader: Decimal
    delta_vs_previous: Decimal


class StandingsResponse(BaseModel):
    match_id: str
    freshness: str
    freshness_updated_at: object | None
    rows: list[StandingRowResponse]
