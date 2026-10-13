"""Regression tests for correctness defects found in the 2026-10-13 audit.

Each test fails against the pre-fix code. See AUDIT.md for the findings.
"""
from datetime import datetime

from sqlalchemy import select

from app.models.leagues import League
from app.models.matches import Match, MatchEvent
from app.models.scoring import FantasyTeam, FantasyTeamPlayer, PointsLedger, ScoringRuleset
from app.models.standings import StandingsSnapshot
from app.models.users import User
from app.services.scoring.pipeline import process_match_event
from app.services.standings_service import recalculate_and_snapshot
from app.services.event_adapter import find_existing_event
from tests.test_pipeline import _payload, _total


def _second_match_same_event(session, seeded, rules):
    """A second league watching the same fixture, with its own team.

    This is the V1 scenario: providers reuse event IDs across feeds, and two
    leagues can legitimately watch the same real match.
    """
    other_user = User(display_name="Rahul")
    session.add(other_user)
    session.flush()
    league = League(name="Other Circle", owner_user_id=other_user.id, invite_code_hash="h2")
    match = Match(title="IND vs AUS (second feed)", status="LIVE")
    session.add_all([league, match])
    session.flush()
    ruleset = session.scalar(
        select(ScoringRuleset).where(ScoringRuleset.name == rules["name"])
    )
    team = FantasyTeam(
        league_id=league.id,
        user_id=other_user.id,
        match_id=match.id,
        name="Rahul XI",
        ruleset_id=ruleset.id,
    )
    session.add(team)
    session.flush()
    # Points go to rostered players, so the second team needs a roster too.
    for player_id, role, captain, vice in [
        ("p1", "BAT", True, False),
        ("p2", "BOWL", False, True),
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
    session.flush()
    return {"match_id": match.id, "team_id": team.id}


class TestEventIdempotencyIsPerMatch:
    """V1: a duplicate in one match must not discard a real event in another."""

    def test_same_provider_event_id_in_two_matches_both_score(
        self, session, seeded, rules
    ) -> None:
        other = _second_match_same_event(session, seeded, rules)
        shared_id = "evt:shared:0001"

        first = process_match_event(
            session, _payload(match_id=seeded["match_id"], provider_event_id=shared_id, runs=4)
        )
        second = process_match_event(
            session, _payload(match_id=other["match_id"], provider_event_id=shared_id, runs=4)
        )
        session.commit()

        assert first["status"] == "scored"
        assert second["status"] == "scored", (
            "second match's event was treated as a duplicate of the first match's"
        )
        # Both teams must have scored; before the fix the second scored zero.
        assert _total(session, seeded["team_id"]) > 0
        assert _total(session, other["team_id"]) > 0
        assert session.query(MatchEvent).count() == 2

    def test_duplicate_within_the_same_match_is_still_dropped(
        self, session, seeded
    ) -> None:
        payload = _payload(match_id=seeded["match_id"], provider_event_id="evt:dup:1", runs=4)
        assert process_match_event(session, payload)["status"] == "scored"
        session.commit()

        again = process_match_event(session, payload)
        assert again["status"] == "duplicate"
        assert session.query(MatchEvent).count() == 1

    def test_lookup_is_scoped_by_match(self, session, seeded, rules) -> None:
        other = _second_match_same_event(session, seeded, rules)
        shared_id = "evt:scoped:1"
        process_match_event(
            session, _payload(match_id=seeded["match_id"], provider_event_id=shared_id, runs=4)
        )
        session.commit()

        assert find_existing_event(session, seeded["match_id"], shared_id) is not None
        assert find_existing_event(session, other["match_id"], shared_id) is None


class TestSnapshotBatchOrdering:
    """V2: the "previous" batch must not depend on a same-second timestamp tie."""

    def test_consecutive_batches_report_stable_deltas(self, session, seeded) -> None:
        match_id = seeded["match_id"]
        team_id = seeded["team_id"]

        # Three scoring events, each snapshotting immediately — so all three
        # batches land in the same wall-clock second, which used to make the
        # "previous batch" choice arbitrary.
        deltas = []
        for i in range(3):
            process_match_event(
                session,
                _payload(
                    match_id=match_id,
                    provider_event_id=f"evt:seq:{i}",
                    sequence=i + 1,
                    runs=1,
                ),
            )
            rows, _ = recalculate_and_snapshot(session, match_id)
            session.commit()
            deltas.append(
                float(next(r for r in rows if r.fantasy_team_id == team_id).delta_vs_previous)
            )

        assert deltas[0] == 2.0, f"first delta should be the full total, got {deltas[0]}"
        assert deltas[1:] == [2.0, 2.0], (
            f"each subsequent delta should be exactly one ball, got {deltas}"
        )

    def test_batch_seq_is_monotonic(self, session, seeded) -> None:
        match_id = seeded["match_id"]
        for i in range(3):
            recalculate_and_snapshot(session, match_id)
            session.commit()

        seqs = list(
            session.scalars(
                select(StandingsSnapshot.batch_seq)
                .where(StandingsSnapshot.match_id == match_id)
                .order_by(StandingsSnapshot.batch_seq)
            )
        )
        assert seqs == sorted(seqs)
        assert len(set(seqs)) == 3, f"each batch needs a distinct seq, got {seqs}"

    def test_repeated_reads_without_new_events_report_zero_delta(
        self, session, seeded
    ) -> None:
        """A read must not change what the next read reports.

        Every recalculate writes a snapshot, so with no new scoring events each
        successive read must report a zero delta. Before the fix, same-second
        batches made the "previous" batch arbitrary and this drifted upward.
        """
        match_id = seeded["match_id"]
        process_match_event(
            session,
            _payload(match_id=match_id, provider_event_id="evt:newest:1", sequence=1, runs=4),
        )
        # First snapshot: no prior batch, so the delta is the full total.
        first_read, _ = recalculate_and_snapshot(session, match_id)
        session.commit()
        assert float(first_read[0].delta_vs_previous) > 0, (
            "the first snapshot should report the full total as its delta"
        )

        # Every read after that has nothing new to report and must stay at zero.
        for i in range(3):
            rows, _ = recalculate_and_snapshot(session, match_id)
            session.commit()
            delta = float(rows[0].delta_vs_previous)
            assert delta == 0.0, (
                f"read {i + 2} reported a non-zero delta ({delta}) with no new events"
            )
