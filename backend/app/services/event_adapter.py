from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.matches import MatchEvent

WICKET_KINDS = {"BOWLED", "CAUGHT", "LBW", "STUMPED", "RUN_OUT"}


class InvalidEventError(ValueError):
    """Raised when a provider/demo event payload fails validation."""


def _parse_timestamp(value: str) -> datetime:
    try:
        parsed = datetime.fromisoformat(value)
    except (TypeError, ValueError) as exc:
        raise InvalidEventError("occurred_at must be an ISO-8601 timestamp") from exc
    return parsed.astimezone(timezone.utc).replace(tzinfo=None)


def validate_payload(payload: dict) -> dict:
    """Checks and normalizes an incoming event. Raises InvalidEventError."""
    for field in ("match_id", "provider_event_id", "sequence", "occurred_at",
                  "batter", "bowler"):
        if payload.get(field) is None:
            raise InvalidEventError(f"{field} is required")

    runs = payload.get("runs", 0)
    if not isinstance(runs, int) or runs < 0:
        raise InvalidEventError("runs must be a non-negative integer")

    wicket = bool(payload.get("wicket"))
    kind = payload.get("wicket_kind")
    if wicket:
        if kind not in WICKET_KINDS:
            raise InvalidEventError(f"wicket_kind must be one of {sorted(WICKET_KINDS)}")
        if kind in {"CAUGHT", "STUMPED", "RUN_OUT"} and not payload.get("fielder"):
            raise InvalidEventError(f"{kind} requires a fielder")
    elif payload.get("fielder") is not None:
        raise InvalidEventError("fielder is only valid on a wicket event")

    return {
        "match_id": payload["match_id"],
        "provider_event_id": payload["provider_event_id"],
        "sequence": payload["sequence"],
        "occurred_at": _parse_timestamp(payload["occurred_at"]),
        "event_type": payload.get("event_type", "BALL"),
        "batter": payload["batter"],
        "bowler": payload["bowler"],
        "fielder": payload.get("fielder"),
        "runs": runs,
        "wicket": wicket,
        "wicket_kind": kind,
    }


def find_existing_event(
    session: Session, match_id: str, provider_event_id: str
) -> MatchEvent | None:
    """Idempotency is per match, not global.

    Two leagues can watch the same real fixture, and providers reuse event IDs
    across feeds. Scoping by ``match_id`` keeps a duplicate in one match from
    silently discarding a genuine event in another.
    """
    return session.scalar(
        select(MatchEvent).where(
            MatchEvent.match_id == match_id,
            MatchEvent.provider_event_id == provider_event_id,
        )
    )
