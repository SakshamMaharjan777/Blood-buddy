#!/usr/bin/env python3
"""
Sync the BloodBuddy frontend into the Spring Boot static folder.

The frontend must live at backend/src/main/resources/static/ so the Spring app
serves it on the same origin as /api/** — which is exactly what bb-api.js
expects (BASE = '', origin-relative paths, no CORS needed).

Run from the repo root:   python tools/sync_frontend_to_backend.py
Re-run after any frontend edit; --clean removes the folder first.

NOT copied: docs/, tools/, resptest/ harness dumps, .git, report artifacts.
resptest/ IS copied (root-absolute paths need the served origin), and root
404.html goes to the root of static/ where Spring serves it for unknown paths.
"""
import argparse
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT
DEST = ROOT / "backend" / "src" / "main" / "resources" / "static"

COPY_DIRS = ["CODE", "CSS", "images", "resptest"]
COPY_FILES = ["404.html"]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--clean", action="store_true",
                    help="delete static/ before copying")
    args = ap.parse_args()

    if args.clean and DEST.exists():
        shutil.rmtree(DEST)
        print(f"removed {DEST.relative_to(ROOT)}")

    for d in COPY_DIRS:
        src_dir = SRC / d
        if not src_dir.exists():
            print(f"WARNING: missing {src_dir}, skipped", file=sys.stderr)
            continue
        shutil.copytree(src_dir, DEST / d, dirs_exist_ok=True)
        print(f"copied {d}/ -> {DEST.relative_to(ROOT)}/{d}/")

    for f in COPY_FILES:
        src_file = SRC / f
        if not src_file.exists():
            print(f"WARNING: missing {src_file}, skipped", file=sys.stderr)
            continue
        shutil.copy2(src_file, DEST / f)
        print(f"copied {f} -> {DEST.relative_to(ROOT)}/{f}")

    total = sum(1 for p in DEST.rglob("*") if p.is_file())
    print(f"\ndone: {total} files in {DEST.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
