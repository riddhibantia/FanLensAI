from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai.gemini_client import get_llm_client
from app.api.deps import get_current_user, get_session, require_match_access
from app.core.config import get_settings
from app.models.matches import Match, MatchEvent
from app.models.users import User
from app.schemas.events import (
    EventIngestRequest,
    EventIngestResponse,
    EventListResponse,
    EventResponse,
)
from app.services.insight_service import collect_proactive_insights
from app.services.league_service import utcnow
from app.services.scoring.pipeline import process_match_event
from app.services.standings_service import recalculate_and_snapshot

router = APIRouter()


@router.get("/matches/{match_id}/events", response_model=EventListResponse)
def get_events(
    match_id: str,
    after: int | None = Query(default=None, description="Return events with sequence > after"),
    limit: int = Query(default=100, le=500),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> EventListResponse:
    require_match_access(session, match_id, user.id)
    stmt = select(MatchEvent).where(MatchEvent.match_id == match_id)
    if after is not None:
        stmt = stmt.where(MatchEvent.sequence > after)
    events = session.scalars(stmt.order_by(MatchEvent.sequence).limit(limit))
    return EventListResponse(
        match_id=match_id,
        events=[
            EventResponse(
                provider_event_id=e.provider_event_id,
                sequence=e.sequence,
                event_type=e.event_type,
                occurred_at=e.occurred_at,
                payload=e.payload,
                status=e.status,
                superseded_by=e.superseded_by,
            )
            for e in events
        ],
    )


@router.post(
    "/matches/{match_id}/events",
    response_model=EventIngestResponse,
    status_code=status.HTTP_201_CREATED,
)
def ingest_event(
    match_id: str,
    body: EventIngestRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> EventIngestResponse:
    match = session.get(Match, match_id)
    if match is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "match not found")
    require_match_access(session, match_id, user.id)

    result = process_match_event(session, {**body.model_dump(), "match_id": match_id})
    if result["status"] == "duplicate":
        return EventIngestResponse(status="duplicate", ledger_rows=0)

    match.source = "PROVIDER"
    match.source_updated_at = utcnow()
    if match.status in {"SCHEDULED", "FINISHED"}:
        match.status = "LIVE"

    # Proactive insights run before the new snapshot so rank/gap changes compare
    # against the previous state.
    collect_proactive_insights(session, match_id, get_llm_client(get_settings().ai_api_key))
    recalculate_and_snapshot(session, match_id)
    session.commit()
    return EventIngestResponse(
        status="scored",
        event_id=result.get("event_id"),
        ledger_rows=result.get("ledger_rows", 0),
    )
