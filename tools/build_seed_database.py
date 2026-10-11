#!/usr/bin/env python3
"""Rebuild Room's bundled database from the reviewed SQL dump (Python stdlib only)."""

import argparse
import os
from pathlib import Path
import sqlite3
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SQL_DUMP = ROOT / "database/finpulse.sql"
DATABASE_ASSET = ROOT / "app/src/main/assets/database/finpulse.db"


def build(output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    handle, temporary_name = tempfile.mkstemp(suffix=".db", dir=output.parent)
    os.close(handle)
    temporary = Path(temporary_name)
    try:
        connection = sqlite3.connect(temporary)
        try:
            connection.executescript(SQL_DUMP.read_text(encoding="utf-8"))
            if connection.execute("PRAGMA integrity_check").fetchall() != [("ok",)]:
                raise RuntimeError("Seed database integrity check failed")
            if connection.execute("PRAGMA foreign_key_check").fetchall():
                raise RuntimeError("Seed database has invalid foreign keys")
            if connection.execute("PRAGMA user_version").fetchone()[0] != 1:
                raise RuntimeError("Seed database must use Room schema version 1")
        finally:
            connection.close()
        # Publish only a fully validated file. Never open the app's live database here.
        temporary.replace(output)
    finally:
        temporary.unlink(missing_ok=True)
    print(f"Seed database generated: {output}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DATABASE_ASSET)
    build(parser.parse_args().output.resolve())
