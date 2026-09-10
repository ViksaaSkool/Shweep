#!/usr/bin/env python3
"""Guard against website-only assets leaking into the KMP mobile apps.

This project ships two unrelated deliverables from one repository:

  * The KMP app (Android + iOS), built from `composeApp/src` and `iosApp/iosApp`.
  * The GitHub Pages website, built from `docs/`.

Website files (HTML, CSS, video, poster, animation layers) must never end up
inside the app source sets or inside a packaged APK/AAB/.app bundle.

Usage:
  python3 tools/web_asset_guard.py check-source
  python3 tools/web_asset_guard.py check-archive <path> [<path> ...]
"""

import os
import sys
import zipfile

# App source roots that must stay free of website assets.
APP_SOURCE_DIRS = ["composeApp/src", "iosApp/iosApp"]

# Extensions that are website-only within this repository.
FORBIDDEN_EXTENSIONS = {".html", ".htm", ".css", ".mp4", ".webm", ".gif"}

# Filename markers for the landing page assets (regardless of extension).
FORBIDDEN_NAME_MARKERS = ("landing-loop", "landing-poster", "landing_background")

# Directories to skip while walking.
SKIP_DIRS = {".git", ".gradle", "build", ".idea", "node_modules"}


def is_forbidden(name: str) -> bool:
    lower = name.lower()
    ext = os.path.splitext(lower)[1]
    if ext in FORBIDDEN_EXTENSIONS:
        return True
    return any(marker in lower for marker in FORBIDDEN_NAME_MARKERS)


def _iter_archive_entries(path: str):
    """Yield entry names for a zip archive (apk/aab) or a bundle directory (.app)."""
    if os.path.isdir(path):
        for root, dirs, files in os.walk(path):
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
            for name in files:
                yield os.path.join(root, name)
    else:
        with zipfile.ZipFile(path) as zf:
            for name in zf.namelist():
                yield name


def check_source() -> int:
    violations = []
    for src_dir in APP_SOURCE_DIRS:
        if not os.path.isdir(src_dir):
            print(f"warning: source dir not found: {src_dir}", file=sys.stderr)
            continue
        for root, dirs, files in os.walk(src_dir):
            dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
            for name in files:
                if is_forbidden(name):
                    violations.append(os.path.join(root, name))

    if violations:
        print("ERROR: website assets found inside app source sets:")
        for v in sorted(violations):
            print(f"  - {v}")
        print(
            "Keep landing page files under docs/ (pages, video, poster) and "
            "art/background/ (source artwork).",
            file=sys.stderr,
        )
        return 1

    print("OK: no website assets in app source sets.")
    return 0


def check_archive(paths) -> int:
    if not paths:
        print("error: provide at least one archive or bundle path", file=sys.stderr)
        return 2

    violations = []
    for path in paths:
        if not os.path.exists(path):
            print(f"error: path not found: {path}", file=sys.stderr)
            return 2
        for entry in _iter_archive_entries(path):
            name = os.path.basename(entry)
            if is_forbidden(name):
                violations.append(f"{path}: {entry}")

    if violations:
        print("ERROR: website assets found inside packaged app artifact:")
        for v in sorted(violations):
            print(f"  - {v}")
        return 1

    print("OK: no website assets in packaged artifacts.")
    return 0


def main(argv) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    command = argv[1]
    if command == "check-source":
        return check_source()
    if command == "check-archive":
        return check_archive(argv[2:])
    print(f"unknown command: {command}", file=sys.stderr)
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
