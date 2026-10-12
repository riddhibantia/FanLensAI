import json
import re
from decimal import Decimal, InvalidOperation

MAX_TEXT_LENGTH = 280


class InvalidInsightError(ValueError):
    """LLM output failed schema, number, or event-ID validation."""


_NUMBER_RE = re.compile(r"\d+(?:\.\d+)?")
_EVENT_ID_RE = re.compile(r"\b(?:demo|evt):[A-Za-z0-9_.-]+\b")


def _allowed_numbers(facts: dict) -> list[Decimal]:
    allowed: list[Decimal] = []

    def add(value) -> None:
        try:
            allowed.append(Decimal(str(value)))
        except (InvalidOperation, ValueError, TypeError):
            pass

    for key in (
        "rank",
        "previous_rank",
        "total_points",
        "previous_total_points",
        "gap_to_leader",
        "previous_gap_to_leader",
        "gap_to_rank_ahead",
        "leader_total_points",
    ):
        if key in facts:
            add(facts[key])
    for entry in facts.get("contributing_events", []):
        add(entry.get("points", 0))
    return allowed


def validate_llm_output(raw: str, facts: dict) -> str:
    try:
        data = json.loads(raw)
    except (json.JSONDecodeError, TypeError) as exc:
        raise InvalidInsightError("output is not JSON") from exc
    if not isinstance(data, dict) or "text" not in data:
        raise InvalidInsightError('output must be an object with a "text" field')

    text = data["text"]
    if not isinstance(text, str) or not text.strip():
        raise InvalidInsightError("text must be a non-empty string")
    if len(text) > MAX_TEXT_LENGTH:
        raise InvalidInsightError(f"text exceeds {MAX_TEXT_LENGTH} characters")

    allowed = _allowed_numbers(facts)
    # Mask known event IDs so digits inside them are not read as number claims.
    scannable = text
    for event_id in facts.get("event_ids", []):
        scannable = scannable.replace(str(event_id), " ")
    for match in _NUMBER_RE.finditer(scannable):
        number = Decimal(match.group())
        if not any(abs(number - candidate) <= Decimal("0.01") for candidate in allowed):
            raise InvalidInsightError(f"number {match.group()} does not match server facts")

    known_ids = set(facts.get("event_ids", []))
    for match in _EVENT_ID_RE.finditer(text):
        if match.group() not in known_ids:
            raise InvalidInsightError(f"unknown event id {match.group()}")

    return text
