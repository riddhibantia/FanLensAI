"""Nightly-style purge of derived rows older than FANLENS_RETENTION_DAYS.

Usage (powershell):
    .\\.venv\\Scripts\\python.exe scripts\\purge_stale.py
"""

from app.core.config import get_settings
from app.db.session import SessionLocal
from app.services.retention import purge_stale_derived_rows


def main() -> None:
    settings = get_settings()
    with SessionLocal() as session:
        counts = purge_stale_derived_rows(session, settings.retention_days)
    print(f"purged (older than {settings.retention_days}d): {counts}")


if __name__ == "__main__":
    main()
