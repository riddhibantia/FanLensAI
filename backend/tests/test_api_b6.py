from fastapi.testclient import TestClient

from tests.test_league_api import auth


def _setup(client: TestClient, session) -> dict:
    """User with a live match, ruleset, roster, and enough access to hit endpoints."""
    import json
    from pathlib import Path

    from app.models.matches import Match
    from app.models.scoring import FantasyTeam, FantasyTeamPlayer, ScoringRuleset

    rules = json.loads(
        Path("app/data/default_ruleset.json").read_text(encoding="utf-8")
    )
    headers = auth(client, "Nikki")
    league = client.post("/v1/leagues", json={"name": "Sunday Smashers"}, headers=headers).json()

    ruleset = ScoringRuleset(name=rules["name"], version=rules["version"], rules=rules)
    session.add(ruleset)
    session.flush()
    match = Match(title="IND vs AUS", status="SCHEDULED")
    session.add(match)
    session.flush()
    team = FantasyTeam(
        league_id=league["id"],
        user_id=league["owner_user_id"],
        match_id=match.id,
        name="Nikki XI",
        ruleset_id=ruleset.id,
    )
    session.add(team)
    session.flush()
    for player_id, role, captain, vice in [
        ("p1", "BAT", True, False),
        ("p2", "BOWL", False, False),
        ("p3", "WK", False, False),
    ]:
        session.add(
            FantasyTeamPlayer(
                fantasy_team_id=team.id,
                player_id=player_id,
                role=role,
                is_captain=captain,
                is_vice=vice,
            )
        )
    session.commit()
    return {"headers": headers, "match_id": match.id, "league_id": league["id"]}


def _event(match_id: str, **overrides) -> dict:
    payload = {
        "provider_event_id": "demo:0001",
        "sequence": 1,
        "occurred_at": "2026-10-10T10:00:00",
        "event_type": "BALL",
        "batter": "p1",
        "bowler": "p2",
        "runs": 4,
        "wicket": False,
    }
    payload.update(overrides)
    return payload


def test_ingest_event_then_read_standings(client: TestClient, session) -> None:
    ctx = _setup(client, session)
    ingested = client.post(
        f"/v1/matches/{ctx['match_id']}/events", json=_event(ctx["match_id"]), headers=ctx["headers"]
    )
    assert ingested.status_code == 201, ingested.text
    assert ingested.json()["status"] == "scored"

    standings = client.get(
        f"/v1/leagues/{ctx['league_id']}/standings?match_id={ctx['match_id']}",
        headers=ctx["headers"],
    )
    assert standings.status_code == 200, standings.text
    body = standings.json()
    assert body["freshness"] == "LIVE"
    row = body["rows"][0]
    assert float(row["total_points"]) == 10.0
    assert row["rank"] == 1


def test_event_ingest_is_idempotent(client: TestClient, session) -> None:
    ctx = _setup(client, session)
    payload = _event(ctx["match_id"])
    first = client.post(
        f"/v1/matches/{ctx['match_id']}/events", json=payload, headers=ctx["headers"]
    )
    second = client.post(
        f"/v1/matches/{ctx['match_id']}/events", json=payload, headers=ctx["headers"]
    )
    assert first.json()["status"] == "scored"
    assert second.json()["status"] == "duplicate"

    standings = client.get(
        f"/v1/leagues/{ctx['league_id']}/standings?match_id={ctx['match_id']}",
        headers=ctx["headers"],
    )
    assert float(standings.json()["rows"][0]["total_points"]) == 10.0


def test_event_history_cursor(client: TestClient, session) -> None:
    ctx = _setup(client, session)
    for sequence in (1, 2, 3):
        client.post(
            f"/v1/matches/{ctx['match_id']}/events",
            json=_event(ctx["match_id"], provider_event_id=f"demo:000{sequence}", sequence=sequence),
            headers=ctx["headers"],
        )

    all_events = client.get(
        f"/v1/matches/{ctx['match_id']}/events", headers=ctx["headers"]
    ).json()["events"]
    assert [e["sequence"] for e in all_events] == [1, 2, 3]

    after_one = client.get(
        f"/v1/matches/{ctx['match_id']}/events?after=1", headers=ctx["headers"]
    ).json()["events"]
    assert [e["sequence"] for e in after_one] == [2, 3]


def test_outsider_cannot_read_events(client: TestClient, session) -> None:
    ctx = _setup(client, session)
    outsider = auth(client, "Outsider")
    response = client.get(
        f"/v1/matches/{ctx['match_id']}/events", headers=outsider
    )
    assert response.status_code == 403


def test_preferences_patch_persists(client: TestClient, session) -> None:
    headers = auth(client, "Nikki")
    response = client.patch(
        "/v1/me/preferences",
        json={
            "companion_mode": "ALWAYS_REACTIVE",
            "overlay_enabled": True,
            "refresh_policy": "AUTO",
            "trigger_categories": ["RANK_SWAP", "GAP_THRESHOLD"],
            "gap_threshold": 30,
            "ai_enabled": True,
            "tone": "BANTER",
        },
        headers=headers,
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["companion_mode"] == "ALWAYS_REACTIVE"
    assert body["overlay_enabled"] is True
    assert body["refresh_policy"] == "AUTO"
    assert body["trigger_categories"] == ["RANK_SWAP", "GAP_THRESHOLD"]
    assert body["gap_threshold"] == 30

    # Partial PATCH: only changes the named field.
    patched = client.patch(
        "/v1/me/preferences", json={"companion_mode": "TAP_TO_ACTIVATE"}, headers=headers
    )
    assert patched.status_code == 200
    body = patched.json()
    assert body["companion_mode"] == "TAP_TO_ACTIVATE"
    assert body["refresh_policy"] == "AUTO"


def test_preferences_reject_bad_enum(client: TestClient) -> None:
    headers = auth(client, "Nikki")
    response = client.patch(
        "/v1/me/preferences", json={"companion_mode": "LOUD"}, headers=headers
    )
    assert response.status_code == 422
