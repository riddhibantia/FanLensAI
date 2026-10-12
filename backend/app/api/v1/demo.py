import json
from datetime import timedelta
from pathlib import Path

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai.gemini_client import get_llm_client
from app.api.deps import get_current_user, get_session, require_match_access
from app.core.config import get_settings
from app.models.matches import Match, MatchEvent
from app.models.users import User
from app.schemas.events import EventIngestResponse
from app.services.insight_service import collect_proactive_insights
from app.services.league_service import utcnow
from app.services.scoring.pipeline import process_match_event
from app.services.standings_service import recalculate_and_snapshot

router = APIRouter()

DEMO_SCRIPT_PATH = Path(__file__).resolve().parents[2] / "data" / "demo_script.json"


def _load_script() -> list[dict]:
    return json.loads(DEMO_SCRIPT_PATH.read_text(encoding="utf-8"))


@router.post(
    "/demo/matches/{match_id}/events",
    response_model=list[EventIngestResponse],
    status_code=status.HTTP_201_CREATED,
)
def inject_demo_events(
    match_id: str,
    balls: int = Query(default=1, ge=1, le=50),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> list[EventIngestResponse]:
    """Feeds the next scripted balls into a demo match. Safe to call repeatedly."""
    match = session.get(Match, match_id)
    if match is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "match not found")
    require_match_access(session, match_id, user.id)

    script = _load_script()
    max_sequence = session.scalar(
        select(MatchEvent.sequence)
        .where(MatchEvent.match_id == match_id)
        .order_by(MatchEvent.sequence.desc())
        .limit(1)
    ) or 0
    last_occurred = session.scalar(
        select(MatchEvent.occurred_at)
        .where(MatchEvent.match_id == match_id)
        .order_by(MatchEvent.occurred_at.desc())
        .limit(1)
    ) or utcnow()

    results: list[EventIngestResponse] = []
    for index in range(balls):
        sequence = max_sequence + index + 1
        ball = script[(sequence - 1) % len(script)]
        occurred_at = last_occurred + timedelta(seconds=index + 1)
        payload = {
            "match_id": match_id,
            "provider_event_id": f"demo:{match_id}:{sequence:04d}",
            "sequence": sequence,
            "event_type": "BALL",
            "occurred_at": occurred_at.isoformat(),
            **ball,
        }
        result = process_match_event(session, payload)
        results.append(
            EventIngestResponse(
                status="duplicate" if result["status"] == "duplicate" else "scored",
                event_id=result.get("event_id"),
                ledger_rows=result.get("ledger_rows", 0),
            )
        )

    match.source = "DEMO"
    match.status = "LIVE"
    collect_proactive_insights(session, match_id, get_llm_client(get_settings().ai_api_key))
    recalculate_and_snapshot(session, match_id)
    session.commit()
    return results
