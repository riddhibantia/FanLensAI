"""Seeds a demo league with two users, rosters, and a live match.

Run from the backend directory with a database configured:

    .venv\\Scripts\\python.exe scripts\\seed_demo.py

Prints the user tokens the Android app needs (device-token auth).
"""
import json
import sys
from datetime import UTC, datetime, timedelta
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import select

from app.core.security import generate_device_token, generate_invite_code
from app.db.session import SessionLocal
from app.models.leagues import League, LeagueMember
from app.models.matches import Match, MatchEvent
from app.models.scoring import FantasyTeam, FantasyTeamPlayer, ScoringRuleset
from app.models.users import DeviceToken, User, UserPreferences
from app.services.scoring.pipeline import process_match_event
from app.services.standings_service import recalculate_and_snapshot

ROOT = Path(__file__).resolve().parents[1]

ROSTER_BY_USER = {
    "Nikki": [
        ("p1", "BAT", True, False),
        ("p2", "BAT", False, False),
        ("p3", "BAT", False, False),
        ("p4", "BOWL", False, False),
        ("p5", "BOWL", False, False),
        ("p6", "BOWL", False, True),
        ("p7", "WK", False, False),
        ("p8", "AR", False, False),
        ("p9", "AR", False, False),
        ("pA", "AR", False, False),
        ("pB", "BAT", False, False),
    ],
    "Rahul": [
        ("p1", "WK", False, False),
        ("p2", "BAT", True, False),
        ("p3", "BAT", False, False),
        ("p4", "BAT", False, False),
        ("p5", "AR", False, True),
        ("p6", "AR", False, False),
        ("p7", "AR", False, False),
        ("p8", "BOWL", False, False),
        ("p9", "BOWL", False, False),
        ("pA", "BOWL", False, False),
        ("pB", "BOWL", False, False),
    ],
}


def seed() -> dict:
    session = SessionLocal()
    output: dict = {"league_id": "", "match_id": "", "invite_code": "", "tokens": {}}
    try:
        rules_json = json.loads(
            (ROOT / "app" / "data" / "default_ruleset.json").read_text(encoding="utf-8")
        )
        ruleset = session.scalar(
            select(ScoringRuleset).where(
                ScoringRuleset.name == rules_json["name"],
                ScoringRuleset.version == rules_json["version"],
            )
        )
        if ruleset is None:
            ruleset = ScoringRuleset(
                name=rules_json["name"], version=rules_json["version"], rules=rules_json
            )
            session.add(ruleset)
            session.flush()

        users = {}
        for name in ("Nikki", "Rahul"):
            user = session.scalar(select(User).where(User.display_name == name))
            if user is None:
                user = User(display_name=name)
                session.add(user)
                session.flush()
            users[name] = user

        league = session.scalar(select(League).where(League.name == "Sunday Smashers"))
        if league is None:
            raw_invite, code_hash = generate_invite_code()
            league = League(
                name="Sunday Smashers",
                owner_user_id=users["Nikki"].id,
                invite_code_hash=code_hash,
            )
            session.add(league)
            session.flush()
            session.add(
                LeagueMember(league_id=league.id, user_id=users["Nikki"].id, role="OWNER")
            )
            session.add(
                LeagueMember(league_id=league.id, user_id=users["Rahul"].id, role="MEMBER")
            )
            output["invite_code"] = raw_invite

        match = session.scalar(select(Match).where(Match.title == "IND vs AUS (demo)"))
        if match is None:
            match = Match(title="IND vs AUS (demo)", status="LIVE", source="DEMO")
            session.add(match)
            session.flush()

        for name, entries in ROSTER_BY_USER.items():
            existing_team = session.scalar(
                select(FantasyTeam).where(
                    FantasyTeam.match_id == match.id, FantasyTeam.user_id == users[name].id
                )
            )
            if existing_team is not None:
                continue
            team = FantasyTeam(
                league_id=league.id,
                user_id=users[name].id,
                match_id=match.id,
                name=f"{name} XI",
                ruleset_id=ruleset.id,
            )
            session.add(team)
            session.flush()
            for player_id, role, captain, vice in entries:
                session.add(
                    FantasyTeamPlayer(
                        fantasy_team_id=team.id,
                        player_id=player_id,
                        role=role,
                        is_captain=captain,
                        is_vice=vice,
                    )
                )

        # Nikki starts Always Reactive so the demo shows insights; Rahul stays tap-only.
        prefs = session.get(UserPreferences, users["Nikki"].id)
        if prefs is None:
            session.add(
                UserPreferences(
                    user_id=users["Nikki"].id,
                    companion_mode="ALWAYS_REACTIVE",
                    trigger_categories=["RANK_SWAP", "GAP_THRESHOLD", "MATCH_END"],
                    ai_enabled=True,
                    tone="BANTER",
                )
            )

        # Bowl the scripted balls through the real scoring pipeline, so every
        # number the demo shows was computed by the engine rather than written
        # into a fixture. Re-running is safe: the pipeline is idempotent.
        script = json.loads(
            (ROOT / "app" / "data" / "demo_script.json").read_text(encoding="utf-8")
        )
        start_sequence = (
            session.scalar(
                select(MatchEvent.sequence)
                .where(MatchEvent.match_id == match.id)
                .order_by(MatchEvent.sequence.desc())
                .limit(1)
            )
            or 0
        )
        scored = 0
        for offset, ball in enumerate(script, start=1):
            payload = {
                "match_id": match.id,
                "provider_event_id": f"seed-{match.id}-{start_sequence + offset}",
                "sequence": start_sequence + offset,
                "occurred_at": (
                    datetime.now(UTC) - timedelta(seconds=len(script) - offset)
                ).isoformat(),
                "batter": ball["batter"],
                "bowler": ball["bowler"],
                "fielder": ball.get("fielder"),
                "runs": ball.get("runs", 0),
                "wicket": ball.get("wicket", False),
                "wicket_kind": ball.get("wicket_kind"),
            }
            result = process_match_event(session, payload)
            if result.get("status") != "duplicate":
                scored += 1
        output["balls_scored"] = scored

        rows, _ = recalculate_and_snapshot(session, match.id)
        output["standings"] = [
            {"rank": r.rank, "team": r.team_name, "points": str(r.total_points)}
            for r in rows
        ]

        for name in ("Nikki", "Rahul"):
            raw, token_hash = generate_device_token()
            session.add(DeviceToken(user_id=users[name].id, token_hash=token_hash))
            output["tokens"][name] = raw

        session.commit()
        output["league_id"] = league.id
        output["match_id"] = match.id
    finally:
        session.close()
    return output


if __name__ == "__main__":
    result = seed()
    print("Seed complete.")
    print(f"league_id: {result['league_id']}  match_id: {result['match_id']}")
    if result["invite_code"]:
        print(f"invite code (show once): {result['invite_code']}")
    print(f"balls scored this run: {result.get('balls_scored', 0)}")
    if result.get("standings"):
        print("standings:")
        for row in result["standings"]:
            print(f"  #{row['rank']} {row['team']}: {row['points']} pts")
    for name, token in result["tokens"].items():
        print(f"token[{name}]: {token}")
