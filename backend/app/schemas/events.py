from datetime import datetime

from pydantic import BaseModel


class EventResponse(BaseModel):
    provider_event_id: str
    sequence: int
    event_type: str
    occurred_at: datetime
    payload: dict
    status: str
    superseded_by: str | None = None


class EventListResponse(BaseModel):
    match_id: str
    events: list[EventResponse]


class EventIngestRequest(BaseModel):
    provider_event_id: str
    sequence: int
    occurred_at: str
    batter: str
    bowler: str
    fielder: str | None = None
    runs: int = 0
    wicket: bool = False
    wicket_kind: str | None = None
    event_type: str = "BALL"


class EventIngestResponse(BaseModel):
    status: str
    event_id: str | None = None
    ledger_rows: int = 0
