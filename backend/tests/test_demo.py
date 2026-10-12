from fastapi.testclient import TestClient

from app.core.rate_limit import reset
from tests.test_api_b6 import _setup
from tests.test_league_api import auth


def test_demo_feed_injects_and_scores(client: TestClient, session) -> None:
    reset()
    ctx = _setup(client, session)
    response = client.post(
        f"/v1/demo/matches/{ctx['match_id']}/events?balls=5", headers=ctx["headers"]
    )
    assert response.status_code == 201, response.text
    results = response.json()
    assert len(results) == 5
    assert all(r["status"] == "scored" for r in results)

    standings = client.get(
        f"/v1/leagues/{ctx['league_id']}/standings?match_id={ctx['match_id']}",
        headers=ctx["headers"],
    ).json()
    assert standings["freshness"] == "DEMO"
    assert float(standings["rows"][0]["total_points"]) > 0


def test_demo_feed_replay_is_idempotent(client: TestClient, session) -> None:
    reset()
    ctx = _setup(client, session)
    first = client.post(
        f"/v1/demo/matches/{ctx['match_id']}/events?balls=3", headers=ctx["headers"]
    ).json()
    assert all(r["status"] == "scored" for r in first)

    events = client.get(
        f"/v1/matches/{ctx['match_id']}/events", headers=ctx["headers"]
    ).json()["events"]
    assert len(events) == 3

    second = client.post(
        f"/v1/demo/matches/{ctx['match_id']}/events?balls=3", headers=ctx["headers"]
    ).json()
    assert all(r["status"] == "scored" for r in second)

    events = client.get(
        f"/v1/matches/{ctx['match_id']}/events", headers=ctx["headers"]
    ).json()["events"]
    assert len(events) == 6  # feed always advances; never replays the same balls


def test_demo_feed_requires_membership(client: TestClient, session) -> None:
    reset()
    ctx = _setup(client, session)
    outsider = auth(client, "Outsider")
    response = client.post(
        f"/v1/demo/matches/{ctx['match_id']}/events?balls=1", headers=outsider
    )
    assert response.status_code == 403


def test_join_endpoint_rate_limits(client: TestClient, session) -> None:
    reset()
    owner = auth(client, "Nikki")
    league = client.post("/v1/leagues", json={"name": "Spam Test"}, headers=owner).json()
    friend = auth(client, "Spammer")

    codes = [client.post(
        f"/v1/leagues/{league['id']}/join",
        json={"invite_code": "wrong"},
        headers=friend,
    ).status_code for _ in range(12)]
    assert 403 in codes
    assert 429 in codes, "the 11th+ call must hit the rate limit"
