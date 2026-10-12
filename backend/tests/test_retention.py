from datetime import timedelta

from app.models.insights import AIInsight
from app.models.standings import StandingsSnapshot
from app.services.league_service import utcnow
from app.services.retention import purge_stale_derived_rows


def _insight(session, seeded, days_old: int) -> AIInsight:
    row = AIInsight(
        league_id="league",
        user_id=seeded["user_id"],
        match_id=seeded["match_id"],
        trigger_type="RANK_SWAP",
        event_ids=[],
        facts={},
        text="old insight",
        source="TEMPLATE",
    )
    session.add(row)
    session.flush()
    row.created_at = utcnow() - timedelta(days=days_old)
    session.commit()
    return row


def _snapshot(session, seeded, days_old: int) -> StandingsSnapshot:
    row = StandingsSnapshot(
        league_id="league",
        match_id=seeded["match_id"],
        fantasy_team_id=seeded["team_id"],
        user_id=seeded["user_id"],
        batch_id="batch-old",
        total_points=10,
        rank=1,
        gap_to_leader=0,
        delta_vs_previous=0,
    )
    session.add(row)
    session.flush()
    row.captured_at = utcnow() - timedelta(days=days_old)
    session.commit()
    return row


def test_purge_removes_only_stale_rows(session, seeded):
    old_insight_id = _insight(session, seeded, days_old=100).id
    fresh_insight_id = _insight(session, seeded, days_old=1).id
    old_snap_id = _snapshot(session, seeded, days_old=100).id
    fresh_snap_id = _snapshot(session, seeded, days_old=1).id
    session.expunge_all()

    counts = purge_stale_derived_rows(session, older_than_days=90)

    assert counts == {"ai_insights": 1, "standings_snapshots": 1}
    assert session.get(AIInsight, old_insight_id) is None
    assert session.get(AIInsight, fresh_insight_id) is not None
    assert session.get(StandingsSnapshot, old_snap_id) is None
    assert session.get(StandingsSnapshot, fresh_snap_id) is not None


def test_delete_me_requires_auth(client):
    response = client.delete("/v1/me")
    assert response.status_code == 401


def test_delete_me_removes_user_and_revokes_tokens(client, session):
    from app.core.security import generate_device_token
    from app.models.users import DeviceToken, User, UserPreferences

    created = client.post(
        "/v1/auth/device", json={"display_name": "Temp", "device_id": "test-device"}
    )
    assert created.status_code == 200
    user_id = created.json()["user_id"]
    token = created.json()["token"]

    session.add(UserPreferences(user_id=user_id))
    session.commit()

    response = client.delete("/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 200
    assert response.json()["deleted"]["users"] == 1

    assert session.get(User, user_id) is None
    assert session.get(UserPreferences, user_id) is None
    stored = session.query(DeviceToken).filter_by(user_id=user_id).all()
    assert stored and all(t.revoked_at is not None for t in stored)

    # Revoked token no longer authenticates.
    again = client.delete("/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert again.status_code == 401
    assert generate_device_token  # silence linters about unused import scope
