from fastapi import APIRouter

from app.api.v1 import (
    account,
    auth,
    demo,
    events,
    insights,
    leagues,
    preferences,
    rosters,
    rulesets,
    standings,
)

router = APIRouter()
router.include_router(account.router)
router.include_router(auth.router)
router.include_router(leagues.router)
router.include_router(rosters.router)
router.include_router(rulesets.router)
router.include_router(standings.router)
router.include_router(events.router)
router.include_router(demo.router)
router.include_router(insights.router)
router.include_router(preferences.router)
