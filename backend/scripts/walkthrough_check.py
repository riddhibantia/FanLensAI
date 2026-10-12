"""Live DoD walkthrough: demo feed -> standings -> explain -> replay is a no-op."""

import json
import urllib.request

BASE = "http://127.0.0.1:8000"
LEAGUE = "3df6a6f3-2c91-431b-8e8d-f37d34c2ca86"
MATCH = "ca228a6f-d4de-4003-b9f9-9711be5682e7"
TOKEN = "IzwGPncJkYOtzrr9Q8YM8e2Q30WAqdULwzMNG_kHuQA"


def call(method, path, body=None):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode() if body is not None else b"",
        method=method,
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Content-Type": "application/json",
        },
    )
    with urllib.request.urlopen(req) as resp:
        return resp.status, json.loads(resp.read().decode() or "{}")


def totals(rows):
    return {r["team_name"]: r["total_points"] for r in rows}


s, health = call("GET", "/healthz")
print("health:", health)
assert health["status"] == "ok"

s, first = call("POST", f"/v1/demo/matches/{MATCH}/events?balls=5")
print("demo inject #1:", [(e.get("status")) for e in first])
assert all(e["status"] in ("scored", "accepted") for e in first), first

s, standings = call("GET", f"/v1/leagues/{LEAGUE}/standings?match_id={MATCH}")
print("freshness:", standings["freshness"], "| rows:", totals(standings["rows"]))
assert standings["freshness"] in ("LIVE", "DELAYED", "STALE", "DEMO")
before = totals(standings["rows"])

s, insight = call(
    "POST", f"/v1/leagues/{LEAGUE}/insights/explain", {"question": "why did I move?"}
)
print("explain:", insight["source"], "|", insight["text"][:160])
assert insight["text"], "explain returned empty text"

# Replay: re-ingest the first provider event directly -> must be a duplicate no-op.
s, events = call("GET", f"/v1/matches/{MATCH}/events?after=0")
first_event = events["events"][0]
replay_body = {
    "provider_event_id": first_event["provider_event_id"],
    "sequence": first_event["sequence"],
    "occurred_at": first_event["occurred_at"],
    "batter": "p1",
    "bowler": "p4",
    "runs": 6,
    "wicket": False,
    "event_type": first_event["event_type"],
}
s, replay = call("POST", f"/v1/matches/{MATCH}/events", replay_body)
print("replay status:", replay.get("status"))
assert replay.get("status") == "duplicate", replay

s, standings2 = call("GET", f"/v1/leagues/{LEAGUE}/standings?match_id={MATCH}")
after = totals(standings2["rows"])
print("totals unchanged after replay:", after == before)
assert after == before, (before, after)

print("WALKTHROUGH OK")
