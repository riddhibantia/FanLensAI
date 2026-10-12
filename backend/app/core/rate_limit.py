import time
from collections import defaultdict, deque

from fastapi import HTTPException, status

MAX_CALLS = 10
WINDOW_SECONDS = 60.0

_buckets: dict[str, deque[float]] = defaultdict(deque)


def _key(host: str, path: str) -> str:
    return f"{host}:{path}"


def check(host: str, path: str, now: float | None = None) -> None:
    now = time.monotonic() if now is None else now
    bucket = _buckets[_key(host, path)]
    while bucket and now - bucket[0] > WINDOW_SECONDS:
        bucket.popleft()
    if len(bucket) >= MAX_CALLS:
        raise HTTPException(
            status.HTTP_429_TOO_MANY_REQUESTS, "rate limit exceeded, slow down"
        )
    bucket.append(now)


def reset() -> None:
    _buckets.clear()
