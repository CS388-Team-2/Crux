"""Export a small Kilter feed catalog from a BoardLib kilter.db.

The full database stays outside this repo. This script writes
feed_climbs.json next to itself. Repeat it with:

    python export_feed_climbs.py
    python export_feed_climbs.py PATH_TO_kilter.db

Grade mapping: climb_stats.display_difficulty is rounded to the nearest
integer and joined to difficulty_grades.difficulty. The stored grade is the
V-scale token in boulder_name (the text after "/"), so "6b/V4" is stored
as "V4". Climbs are Kilter Board Original boulders at 40 degrees with at
least 3 ascents, most-climbed first, capped at 300.
"""

import json
import sqlite3
import sys
from pathlib import Path

LIMIT = 300
ANGLE = 40
MIN_ASCENTS = 3
KILTER_ORIGINAL_LAYOUT_ID = 1

OUTPUT = Path(__file__).with_name("feed_climbs.json")
DEFAULT_DB = Path(__file__).resolve().parents[4].parent / "kilter-data" / "kilter.db"

QUERY = """
SELECT
    c.uuid,
    c.name,
    g.boulder_name,
    cs.angle
FROM climb_stats AS cs
JOIN climbs AS c ON c.uuid = cs.climb_uuid
JOIN difficulty_grades AS g
    ON g.difficulty = CAST(ROUND(cs.display_difficulty) AS INTEGER)
WHERE cs.angle = ?
  AND c.layout_id = ?
  AND c.is_draft = 0
  AND c.is_listed = 1
  AND cs.ascensionist_count >= ?
ORDER BY cs.ascensionist_count DESC, c.name ASC
LIMIT ?
"""


def v_grade(boulder_name: str) -> str:
    token = boulder_name.split("/")[-1].strip()
    if len(token) >= 2 and token[0] == "V" and token[1:].isdigit():
        return token
    raise ValueError(f"boulder_name {boulder_name!r} has no V-grade")


def main() -> None:
    db_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_DB
    if not db_path.is_file():
        raise SystemExit(f"kilter.db not found: {db_path}")

    with sqlite3.connect(db_path) as conn:
        rows = conn.execute(
            QUERY,
            (ANGLE, KILTER_ORIGINAL_LAYOUT_ID, MIN_ASCENTS, LIMIT),
        ).fetchall()

    if len(rows) != LIMIT:
        raise SystemExit(f"expected {LIMIT} climbs, got {len(rows)}")

    climbs = []
    seen = set()
    for uuid, name, boulder_name, angle in rows:
        if uuid in seen:
            raise SystemExit(f"duplicate uuid: {uuid}")
        seen.add(uuid)
        if not name or not str(name).strip():
            raise SystemExit(f"missing name for {uuid}")
        climbs.append(
            {
                "uuid": uuid,
                "name": name,
                "grade": v_grade(boulder_name),
                "angle": angle,
            }
        )

    OUTPUT.write_text(
        json.dumps(climbs, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"wrote {len(climbs)} climbs to {OUTPUT}")


if __name__ == "__main__":
    main()
