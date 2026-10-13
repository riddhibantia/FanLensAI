"""add monotonic sequencing to standings snapshot batches

The "previous standings" batch was chosen with `ORDER BY captured_at DESC`.
`captured_at` is a wall-clock server default, so two batches written in the same
second tie and the choice becomes arbitrary — which surfaced as wrong
`delta_vs_previous` values on the leaderboard. `batch_seq` is monotonic, so the
newest batch is always unambiguous.

Note: the per-match event idempotency key (match_id, provider_event_id) is part
of the initial schema revision, so it needs no change here.

Revision ID: b1c2d3e4f5a6
Revises: a88733bcc1f0
Create Date: 2026-10-13 21:40:00.000000
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "b1c2d3e4f5a6"
down_revision: Union[str, None] = "a88733bcc1f0"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

SNAPSHOTS = "standings_snapshots"


def upgrade() -> None:
    op.add_column(SNAPSHOTS, sa.Column("batch_seq", sa.BigInteger(), nullable=True))

    # Backfill so existing rows get a sensible sequence. Both backends expose an
    # insertion-order physical row id (rowid on SQLite, ctid on Postgres) that is
    # already monotonic, so no window function is needed — and SQLite rejects one
    # inside UPDATE SET anyway.
    dialect = op.get_bind().dialect.name
    tiebreak = "rowid" if dialect == "sqlite" else "ctid"
    op.execute(f"UPDATE {SNAPSHOTS} SET batch_seq = {tiebreak}")

    with op.batch_alter_table(SNAPSHOTS, schema=None) as batch:
        batch.alter_column("batch_seq", existing_type=sa.BigInteger(), nullable=False)
        batch.create_index("ix_standings_snapshots_batch_seq", ["batch_seq"])


def downgrade() -> None:
    with op.batch_alter_table(SNAPSHOTS, schema=None) as batch:
        batch.drop_index("ix_standings_snapshots_batch_seq")
        batch.drop_column("batch_seq")
