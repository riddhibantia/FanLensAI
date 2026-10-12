from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

VALID_ROSTER = [
    {"player_id": f"p{i}", "role": "BAT", "is_captain": i == 0, "is_vice": False}
    for i in range(4)
] + [
    {"player_id": f"w{i}", "role": "BOWL", "is_captain": False, "is_vice": i == 0}
    for i in range(4)
] + [
    {"player_id": f"k{i}", "role": "WK", "is_captain": False, "is_vice": False}
    for i in range(2)
] + [{"player_id": "ar0", "role": "AR", "is_captain": False, "is_vice": False}]


def auth(client: TestClient, name: str) -> dict[str, str]:
    response = client.post("/v1/auth/device", json={"display_name": name, "device_id": "dev-1"})
    assert response.status_code == 200, response.text
    token = response.json()["token"]
    return {"Authorization": f"Bearer {token}"}


def test_device_auth_returns_token(client: TestClient) -> None:
    response = client.post(
        "/v1/auth/device", json={"display_name": "Nikki", "device_id": "dev-1"}
    )
    assert response.status_code == 200
    body = response.json()
    assert body["user_id"]
    assert body["token"]


def test_missing_token_rejected(client: TestClient) -> None:
    response = client.post("/v1/leagues", json={"name": "Sunday Smashers"})
    assert response.status_code == 401


def test_invalid_token_rejected(client: TestClient) -> None:
    response = client.post(
        "/v1/leagues",
        json={"name": "Sunday Smashers"},
        headers={"Authorization": "Bearer not-a-real-token"},
    )
    assert response.status_code == 401


def test_create_league_and_join(client: TestClient) -> None:
    owner = auth(client, "Nikki")
    created = client.post("/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner)
    assert created.status_code == 201, created.text
    league = created.json()
    invite = league["invite_code"]
    assert league["name"] == "Sunday Smashers"

    friend = auth(client, "Rahul")
    joined = client.post(
        f"/v1/leagues/{league['id']}/join", json={"invite_code": invite}, headers=friend
    )
    assert joined.status_code == 201, joined.text

    again = client.post(
        f"/v1/leagues/{league['id']}/join", json={"invite_code": invite}, headers=friend
    )
    assert again.status_code == 409


def test_wrong_invite_code_rejected(client: TestClient) -> None:
    owner = auth(client, "Nikki")
    created = client.post("/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner)
    league_id = created.json()["id"]

    friend = auth(client, "Rahul")
    denied = client.post(
        f"/v1/leagues/{league_id}/join",
        json={"invite_code": "wrong-code"},
        headers=friend,
    )
    assert denied.status_code == 403


def test_non_member_cannot_read_league(client: TestClient) -> None:
    owner = auth(client, "Nikki")
    created = client.post("/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner)
    league_id = created.json()["id"]

    outsider = auth(client, "Outsider")
    forbidden = client.get(f"/v1/leagues/{league_id}", headers=outsider)
    assert forbidden.status_code == 403


def test_member_reads_league_summary(client: TestClient) -> None:
    owner = auth(client, "Nikki")
    created = client.post("/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner)
    league = created.json()
    friend = auth(client, "Rahul")
    client.post(
        f"/v1/leagues/{league['id']}/join",
        json={"invite_code": league["invite_code"]},
        headers=friend,
    )

    detail = client.get(f"/v1/leagues/{league['id']}", headers=owner)
    assert detail.status_code == 200
    members = {m["display_name"]: m["role"] for m in detail.json()["members"]}
    assert members == {"Nikki": "OWNER", "Rahul": "MEMBER"}


def test_expired_invite_code_rejected(client: TestClient, session: Session) -> None:
    from datetime import datetime, timedelta

    from app.models.leagues import League

    owner = auth(client, "Nikki")
    created = client.post("/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner)
    league = created.json()

    row = session.get(League, league["id"])
    row.invite_expires_at = datetime.utcnow() - timedelta(hours=1)
    session.commit()

    friend = auth(client, "Rahul")
    response = client.post(
        f"/v1/leagues/{league['id']}/join",
        json={"invite_code": league["invite_code"]},
        headers=friend,
    )
    assert response.status_code == 410


def _seed_match_and_ruleset(session: Session) -> tuple[str, str]:
    from app.models.matches import Match
    from app.models.scoring import ScoringRuleset

    match = Match(title="IND vs AUS", status="LIVE")
    session.add(match)
    session.flush()
    ruleset = ScoringRuleset(name="standard", version="1.0", rules={})
    session.add(ruleset)
    session.commit()
    return match.id, ruleset.id


def test_roster_accepts_valid_team(client: TestClient, session: Session) -> None:
    match_id, ruleset_id = _seed_match_and_ruleset(session)
    owner = auth(client, "Nikki")
    league = client.post(
        "/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner
    ).json()
    headers = owner

    response = client.post(
        f"/v1/leagues/{league['id']}/rosters",
        json={
            "match_id": match_id,
            "team_name": "Nikki XI",
            "ruleset_id": ruleset_id,
            "players": VALID_ROSTER,
        },
        headers=headers,
    )
    assert response.status_code == 201, response.text
    body = response.json()
    assert len(body["players"]) == 11


def test_roster_update_replaces_players(client: TestClient, session: Session) -> None:
    match_id, ruleset_id = _seed_match_and_ruleset(session)
    owner = auth(client, "Nikki")
    league = client.post(
        "/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner
    ).json()
    payload = {
        "match_id": match_id,
        "team_name": "Nikki XI",
        "ruleset_id": ruleset_id,
        "players": VALID_ROSTER,
    }

    first = client.post(
        f"/v1/leagues/{league['id']}/rosters", json=payload, headers=owner
    ).json()
    second = client.post(
        f"/v1/leagues/{league['id']}/rosters", json=payload, headers=owner
    )
    assert second.status_code == 201
    assert second.json()["id"] == first["id"], "same match+user must reuse the team row"
    assert len(second.json()["players"]) == 11


def test_roster_requires_one_captain(client: TestClient, session: Session) -> None:
    match_id, ruleset_id = _seed_match_and_ruleset(session)
    owner = auth(client, "Nikki")
    league = client.post(
        "/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner
    ).json()
    players = [{**p, "is_captain": False} for p in VALID_ROSTER]
    response = client.post(
        f"/v1/leagues/{league['id']}/rosters",
        json={
            "match_id": match_id,
            "team_name": "No Cap",
            "ruleset_id": ruleset_id,
            "players": players,
        },
        headers=owner,
    )
    assert response.status_code == 422


def test_roster_rejects_duplicate_players(client: TestClient, session: Session) -> None:
    match_id, ruleset_id = _seed_match_and_ruleset(session)
    owner = auth(client, "Nikki")
    league = client.post(
        "/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner
    ).json()
    players = [dict(p) for p in VALID_ROSTER]
    players[1]["player_id"] = players[0]["player_id"]
    response = client.post(
        f"/v1/leagues/{league['id']}/rosters",
        json={
            "match_id": match_id,
            "team_name": "Dupe XI",
            "ruleset_id": ruleset_id,
            "players": players,
        },
        headers=owner,
    )
    assert response.status_code == 422


def test_roster_enforces_role_caps(client: TestClient, session: Session) -> None:
    match_id, ruleset_id = _seed_match_and_ruleset(session)
    owner = auth(client, "Nikki")
    league = client.post(
        "/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner
    ).json()
    # 0 BAT, 8 BOWL, 2 WK, 1 AR — BAT minimum is 1 and BOWL maximum is 6
    players = [
        {"player_id": f"w{i}", "role": "BOWL", "is_captain": False, "is_vice": False}
        for i in range(8)
    ] + [
        {"player_id": f"k{i}", "role": "WK", "is_captain": False, "is_vice": False}
        for i in range(2)
    ] + [
        {
            "player_id": "ar0",
            "role": "AR",
            "is_captain": True,
            "is_vice": False,
        }
    ]
    response = client.post(
        f"/v1/leagues/{league['id']}/rosters",
        json={
            "match_id": match_id,
            "team_name": "Bowling Heavy",
            "ruleset_id": ruleset_id,
            "players": players,
        },
        headers=owner,
    )
    assert response.status_code == 422


def test_roster_non_member_forbidden(client: TestClient, session: Session) -> None:
    match_id, ruleset_id = _seed_match_and_ruleset(session)
    owner = auth(client, "Nikki")
    league = client.post(
        "/v1/leagues", json={"name": "Sunday Smashers"}, headers=owner
    ).json()
    outsider = auth(client, "Outsider")
    response = client.post(
        f"/v1/leagues/{league['id']}/rosters",
        json={
            "match_id": match_id,
            "team_name": "Outsider XI",
            "ruleset_id": ruleset_id,
            "players": VALID_ROSTER,
        },
        headers=outsider,
    )
    assert response.status_code == 403
