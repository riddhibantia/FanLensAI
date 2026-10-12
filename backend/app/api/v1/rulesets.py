from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.api.deps import get_current_user, get_session
from app.models.scoring import ScoringRuleset
from app.models.users import User
from app.schemas.rulesets import RulesetResponse

router = APIRouter()


@router.get("/rulesets", response_model=list[RulesetResponse])
def list_rulesets(
    user: User = Depends(get_current_user),
    session: Session = Depends(get_session),
) -> list[RulesetResponse]:
    rulesets = session.scalars(select(ScoringRuleset))
    return [
        RulesetResponse(id=r.id, name=r.name, version=r.version) for r in rulesets
    ]
