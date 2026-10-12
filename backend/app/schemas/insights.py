from pydantic import BaseModel


class InsightResponse(BaseModel):
    id: str
    trigger_type: str
    text: str
    source: str
    event_ids: list[str]
    created_at: str


class InsightListResponse(BaseModel):
    league_id: str
    insights: list[InsightResponse]


class ExplainRequest(BaseModel):
    match_id: str | None = None
    question: str | None = None
