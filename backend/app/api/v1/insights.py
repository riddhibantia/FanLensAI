from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai.gemini_client import get_llm_client
from app.api.deps import (
    get_current_user,
    get_session,
    rate_limit_dependency,
)
from app.core.config import get_settings
from app.models.insights import AIInsight
from app.models.matches import Match
from app.models.scoring import FantasyTeam
from app.models.users import User
from app.schemas.insights import ExplainRequest, InsightListResponse, InsightResponse
from app.services.insight_service import USER_QUESTION, build_facts, generate_insight
from app.services.league_service import require_member
from app.services.standings_service import compute_standings

router = APIRouter()


@router.get("/leagues/{league_id}/insights", response_model=InsightListResponse)
def list_insights(
    league_id: str,
    limit: int = Query(default=20, le=100),
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> InsightListResponse:
    require_member(session, league_id, user.id)
    insights = session.scalars(
        select(AIInsight)
        .where(AIInsight.league_id == league_id, AIInsight.user_id == user.id)
        .order_by(AIInsight.created_at.desc())
        .limit(limit)
    )
    return InsightListResponse(
        league_id=league_id,
        insights=[
            InsightResponse(
                id=i.id,
                trigger_type=i.trigger_type,
                text=i.text,
                source=i.source,
                event_ids=i.event_ids,
                created_at=i.created_at.isoformat() if i.created_at else "",
            )
            for i in insights
        ],
    )


@router.post(
    "/leagues/{league_id}/insights/explain",
    response_model=InsightResponse,
    dependencies=[rate_limit_dependency],
)
def explain(
    league_id: str,
    body: ExplainRequest,
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> InsightResponse:
    require_member(session, league_id, user.id)

    if body.match_id is not None:
        match_id = body.match_id
    else:
        match_id = session.scalar(
            select(FantasyTeam.match_id)
            .where(FantasyTeam.league_id == league_id)
            .order_by(FantasyTeam.created_at.desc())
            .limit(1)
        )
    match = session.get(Match, match_id) if match_id else None
    if match is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "match not found")

    rows = compute_standings(session, match_id)
    row = next((r for r in rows if r.user_id == user.id), None)
    if row is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "no fantasy team for this match")

    facts = build_facts(session, row, None, rows, question=body.question or "")
    insight = generate_insight(
        session,
        league_id,
        user.id,
        match_id,
        USER_QUESTION,
        facts,
        get_llm_client(get_settings().ai_api_key),
        bypass_mode=True,  # a direct question is allowed in either companion mode
    )
    if insight is None:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "ai disabled in preferences")

    return InsightResponse(
        id=insight.id,
        trigger_type=insight.trigger_type,
        text=insight.text,
        source=insight.source,
        event_ids=insight.event_ids,
        created_at=insight.created_at.isoformat() if insight.created_at else "",
    )
