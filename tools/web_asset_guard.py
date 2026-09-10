#!/usr/bin/env python3
"""Guard against website-only assets leaking into the KMP mobile apps.

This project ships two unrelated deliverables from one repository:

  * The KMP app (Android + iOS), built from `composeApp/src` and `iosApp/iosApp`.
  * The GitHub Pages website, built from `docs/`.

Website files (HTML, CSS, video, poster) and repository source artwork under
`art/` must never end up inside the app source sets, inside a packaged
APK/AAB/.app bundle, or outside the Pages artifact.

Usage:
  python3 tools/web_asset_guard.py check-source
  python3 tools/web_asset_guard.py check-pages
  python3 tools/web_asset_guard.py check-archive <path> [<path> ...]
  python3 tools/web_asset_guard.py self-test
"""

import hashlib
import os
import re
import sys
import tempfile
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

# App source roots that must stay free of website and art assets.
APP_SOURCE_DIRS = ("composeApp/src", "iosApp/iosApp")

# Website and source-artwork roots that must never feed an app build.
WEB_ART_DIRS = ("art", "docs")

# Extensions that are website-only within this repository.
FORBIDDEN_EXTENSIONS = {".html", ".htm", ".css", ".mp4", ".webm", ".gif"}

# Filename markers for the landing page assets (regardless of extension).
FORBIDDEN_NAME_MARKERS = ("landing-loop", "landing-poster", "landing_background")

# Media extensions whose bytes are hash-compared against art/ and docs/.
MEDIA_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".mp4", ".webm"}

# Workflow that publishes the website.
PAGES_WORKFLOW = ".github/workflows/pages.yml"

# Directories and files to skip while walking.
SKIP_DIRS = {".git", ".gradle", "build", ".idea", "node_modules", ".opencode"}
SKIP_FILES = {".DS_Store"}

# Patterns that look like build-input redirection into art/ or docs/.
SRC_DIR_RE = re.compile(r"srcDirs?\b")
GRADLE_TARGET_RE = re.compile(r"[\"'](?:\.\./)*(?:art|docs)(?:/|[\"'])")
XCODE_PATH_RE = re.compile(r"path\s*=\s*[\"']?(?:\.\./)*(?:art|docs)[\"']?\s*;")

# References inside docs pages that must stay inside docs/.
HTML_REF_RE = re.compile(r"""(?:href|src|srcset|poster)\s*=\s*["']([^"']+)["']""")
CSS_URL_RE = re.compile(r"""url\(\s*["']?([^"')]+)["']?\s*\)""")
EXTERNAL_RE = re.compile(r"^(?:[a-zA-Z][a-zA-Z0-9+.\-]*:|#|/)")


def is_forbidden(name: str) -> bool:
    lower = name.lower()
    ext = os.path.splitext(lower)[1]
    if ext in FORBIDDEN_EXTENSIONS:
        return True
    return any(marker in lower for marker in FORBIDDEN_NAME_MARKERS)


def _hash_file(path) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 16), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _rel(root: Path, path) -> str:
    try:
        return str(Path(path).relative_to(root))
    except ValueError:
        return str(path)


def _within(child: Path, parent: Path) -> bool:
    try:
        child.relative_to(parent)
        return True
    except ValueError:
        return False


def _scan(base: Path):
    """Walk a directory, returning (regular files, symlinked paths)."""
    files = []
    symlinks = []
    for dirpath, dirnames, filenames in os.walk(base, followlinks=False):
        kept = []
        for d in dirnames:
            if d in SKIP_DIRS:
                continue
            dp = Path(dirpath) / d
            if os.path.islink(dp):
                symlinks.append(dp)
                continue  # never descend into symlinked directories
            kept.append(d)
        dirnames[:] = kept
        for name in filenames:
            if name in SKIP_FILES:
                continue
            fp = Path(dirpath) / name
            if os.path.islink(fp):
                symlinks.append(fp)
            else:
                files.append(fp)
    return files, symlinks


def _web_hash_index(root: Path):
    """Map file hash -> repository path for every file under art/ and docs/."""
    index = {}
    for web_dir in WEB_ART_DIRS:
        base = root / web_dir
        if not base.is_dir():
            continue
        files, _ = _scan(base)
        for f in files:
            index.setdefault(_hash_file(f), f"{web_dir}/{_rel(base, f)}")
    return index


def _config_violations(root: Path):
    """Detect Gradle/Xcode source directories redirected into art/ or docs/."""
    violations = []
    gradle_files = sorted(root.glob("*.gradle.kts")) + sorted(root.glob("composeApp/*.gradle.kts"))
    for path in gradle_files:
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for lineno, line in enumerate(text.splitlines(), 1):
            if SRC_DIR_RE.search(line) and GRADLE_TARGET_RE.search(line):
                violations.append(
                    f"build input redirected into art/ or docs/: {_rel(root, path)}:{lineno}"
                )
    pbx = root / "iosApp/iosApp.xcodeproj/project.pbxproj"
    if pbx.is_file():
        try:
            text = pbx.read_text(encoding="utf-8", errors="replace")
        except OSError:
            text = ""
        for lineno, line in enumerate(text.splitlines(), 1):
            if XCODE_PATH_RE.search(line):
                violations.append(
                    f"build input redirected into art/ or docs/: {_rel(root, pbx)}:{lineno}"
                )
    return violations


def check_source(root: Path = REPO_ROOT) -> int:
    violations = []

    # App source roots must exist: fail closed instead of scanning nothing.
    for src_dir in APP_SOURCE_DIRS:
        if not (root / src_dir).is_dir():
            violations.append(f"missing expected app source root: {src_dir}")

    web_hashes = _web_hash_index(root)
    if not web_hashes:
        print("warning: no files indexed under art/ or docs/", file=sys.stderr)

    for src_dir in APP_SOURCE_DIRS:
        base = root / src_dir
        if not base.is_dir():
            continue
        files, symlinks = _scan(base)
        for link in symlinks:
            violations.append(f"symlink inside app build input: {_rel(root, link)}")
        for f in files:
            if is_forbidden(f.name):
                violations.append(f"website asset in app build input: {_rel(root, f)}")
                continue
            origin = web_hashes.get(_hash_file(f))
            if origin is not None:
                violations.append(
                    f"copy of {origin} in app build input: {_rel(root, f)}"
                )

    violations.extend(_config_violations(root))

    return _report(violations, "OK: no website or art assets in app build inputs.")


def check_pages(root: Path = REPO_ROOT) -> int:
    violations = []
    docs = root / "docs"
    if not docs.is_dir():
        print("ERROR: docs/ directory not found.", file=sys.stderr)
        return 1

    files, symlinks = _scan(docs)
    for link in symlinks:
        violations.append(f"symlink inside docs/: {_rel(root, link)}")

    docs_root = docs.resolve()
    for f in files:
        if f.suffix.lower() not in {".html", ".css"}:
            continue
        try:
            text = f.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        refs = HTML_REF_RE.findall(text) + CSS_URL_RE.findall(text)
        for ref in refs:
            ref = ref.strip()
            if not ref or EXTERNAL_RE.match(ref):
                continue
            resolved = (f.parent / ref).resolve()
            if not _within(resolved, docs_root):
                violations.append(
                    f"docs page references path outside docs/: {_rel(root, f)} -> {ref}"
                )

    # The Pages workflow must upload exactly docs/.
    workflow = root / PAGES_WORKFLOW
    if not workflow.is_file():
        violations.append(f"missing Pages workflow: {PAGES_WORKFLOW}")
    else:
        try:
            text = workflow.read_text(encoding="utf-8", errors="replace")
        except OSError:
            text = ""
        match = re.search(r"^\s*path:\s*(\S+)\s*$", text, re.MULTILINE)
        if not match or match.group(1) != "docs":
            violations.append("Pages workflow must upload exactly 'path: docs'")

    return _report(violations, "OK: Pages boundary is clean.")


def check_archive(paths, root: Path = REPO_ROOT) -> int:
    if not paths:
        print("error: provide at least one archive or bundle path", file=sys.stderr)
        return 2

    web_hashes = _web_hash_index(root)
    violations = []

    for path in paths:
        if not os.path.exists(path):
            print(f"error: path not found: {path}", file=sys.stderr)
            return 2
        if os.path.isdir(path):
            violations.extend(_check_bundle(Path(path), web_hashes))
        else:
            violations.extend(_check_zip(Path(path), web_hashes))

    return _report(violations, "OK: no website assets in packaged artifacts.")


def _check_bundle(bundle: Path, web_hashes):
    violations = []
    bundle_root = bundle.resolve()
    for dirpath, dirnames, filenames in os.walk(bundle, followlinks=False):
        kept = []
        for d in dirnames:
            if d in SKIP_DIRS:
                continue
            dp = Path(dirpath) / d
            if os.path.islink(dp) and not _within(dp.resolve(), bundle_root):
                violations.append(f"symlink escaping bundle: {dp}")
                continue
            kept.append(d)
        dirnames[:] = kept
        for name in filenames:
            if name in SKIP_FILES:
                continue
            fp = Path(dirpath) / name
            if os.path.islink(fp):
                if not _within(fp.resolve(), bundle_root):
                    violations.append(f"symlink escaping bundle: {fp}")
                continue
            if is_forbidden(name):
                violations.append(f"{bundle}: {fp}")
                continue
            if os.path.splitext(name)[1].lower() in MEDIA_EXTENSIONS:
                if _hash_file(fp) in web_hashes:
                    violations.append(
                        f"{bundle}: {fp} (copy of art/ or docs/ asset)"
                    )
    return violations


def _check_zip(archive: Path, web_hashes):
    violations = []
    with zipfile.ZipFile(archive) as zf:
        for name in zf.namelist():
            if name.endswith("/"):
                continue
            base = os.path.basename(name)
            if is_forbidden(base):
                violations.append(f"{archive}: {name}")
                continue
            if os.path.splitext(base)[1].lower() in MEDIA_EXTENSIONS:
                data = zf.read(name)
                if hashlib.sha256(data).hexdigest() in web_hashes:
                    violations.append(
                        f"{archive}: {name} (copy of art/ or docs/ asset)"
                    )
    return violations


def _report(violations, ok_message: str) -> int:
    if violations:
        print("ERROR: boundary violations detected:")
        for v in sorted(set(violations)):
            print(f"  - {v}")
        return 1
    print(ok_message)
    return 0


def _make_sandbox(root: Path):
    """Create a minimal repository skeleton for self-test fixtures."""
    (root / "art").mkdir()
    (root / "art" / "icon.png").write_bytes(b"ART-ICON-BYTES")
    (root / "docs" / "assets").mkdir(parents=True)
    (root / "docs" / "index.html").write_text('<a href="assets/social.png">home</a>')
    (root / "docs" / "assets" / "social.png").write_bytes(b"DOCS-SOCIAL-BYTES")
    (root / "composeApp/src/commonMain/kotlin").mkdir(parents=True)
    (root / "iosApp/iosApp").mkdir(parents=True)
    workflow = root / ".github/workflows/pages.yml"
    workflow.parent.mkdir(parents=True)
    workflow.write_text(
        "jobs:\n"
        "  deploy:\n"
        "    steps:\n"
        "      - uses: actions/upload-pages-artifact@v3\n"
        "        with:\n"
        "          path: docs\n"
    )


def self_test() -> int:
    failures = []

    def expect_failure(name, fn):
        try:
            code = fn()
        except Exception as exc:  # noqa: BLE001 - surface fixture crashes
            failures.append(f"{name}: raised {exc!r}")
            return
        if code == 0:
            failures.append(f"{name}: expected failure, got success")

    def expect_success(name, fn):
        try:
            code = fn()
        except Exception as exc:  # noqa: BLE001
            failures.append(f"{name}: raised {exc!r}")
            return
        if code != 0:
            failures.append(f"{name}: expected success, got exit {code}")

    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        _make_sandbox(root)

        # 1. Clean baseline passes.
        expect_success("clean source", lambda: check_source(root))
        expect_success("clean pages", lambda: check_pages(root))

        # 2. Missing app source root fails closed.
        (root / "iosApp/iosApp").rmdir()
        expect_failure("missing app source root", lambda: check_source(root))
        (root / "iosApp/iosApp").mkdir()

        # 3. Forbidden website extension in app source.
        raw = root / "composeApp/src/androidMain/res/raw"
        raw.mkdir(parents=True)
        (raw / "landing-loop.mp4").write_bytes(b"MP4")
        expect_failure("forbidden extension in app source", lambda: check_source(root))
        (raw / "landing-loop.mp4").unlink()

        # 4. Renamed byte-identical copy of an art asset in app source.
        drawable = root / "composeApp/src/commonMain/composeResources/drawable"
        drawable.mkdir(parents=True)
        (drawable / "hero.png").write_bytes(b"ART-ICON-BYTES")
        expect_failure("renamed art duplicate in app source", lambda: check_source(root))
        (drawable / "hero.png").unlink()

        # 5. Renamed copy inside the iOS synchronized group.
        (root / "iosApp/iosApp/hero.png").write_bytes(b"ART-ICON-BYTES")
        expect_failure("renamed art duplicate in iosApp", lambda: check_source(root))
        (root / "iosApp/iosApp/hero.png").unlink()

        # 6. Symlinked directory pointing into docs.
        link = root / "composeApp/src/commonMain/link_to_docs"
        try:
            link.symlink_to(root / "docs", target_is_directory=True)
            expect_failure("symlinked directory in app source", lambda: check_source(root))
            link.unlink()
        except OSError:
            print("note: symlink fixture skipped (unsupported platform)")

        # 7. Gradle source-set override into art/.
        gradle = root / "composeApp/build.gradle.kts"
        gradle.write_text('android { sourceSets { main { res.srcDir("../../art") } } }')
        expect_failure("gradle srcDir override", lambda: check_source(root))
        gradle.unlink()

        # 8. Xcode path override into docs/.
        pbx = root / "iosApp/iosApp.xcodeproj/project.pbxproj"
        pbx.parent.mkdir(parents=True)
        pbx.write_text("/* obj */ { path = ../docs; }")
        expect_failure("xcode path override", lambda: check_source(root))
        pbx.unlink()

        # 9. Docs page referencing a path outside docs/.
        (root / "docs/index.html").write_text('<img src="../art/icon.png">')
        expect_failure("docs escape reference", lambda: check_pages(root))
        (root / "docs/index.html").write_text('<a href="assets/social.png">home</a>')

        # 10. Pages workflow uploading something other than docs/.
        workflow = root / ".github/workflows/pages.yml"
        workflow.write_text(
            "jobs:\n"
            "  deploy:\n"
            "    steps:\n"
            "      - uses: actions/upload-pages-artifact@v3\n"
            "        with:\n"
            "          path: art\n"
        )
        expect_failure("pages upload path", lambda: check_pages(root))
        workflow.write_text(
            "jobs:\n"
            "  deploy:\n"
            "    steps:\n"
            "      - uses: actions/upload-pages-artifact@v3\n"
            "        with:\n"
            "          path: docs\n"
        )

        # 11. Archive containing a renamed copy of an art asset.
        apk = root / "test.apk"
        with zipfile.ZipFile(apk, "w") as zf:
            zf.writestr("classes.dex", b"DEX")
            zf.writestr("assets/hero.png", b"ART-ICON-BYTES")
        expect_failure("archive renamed duplicate", lambda: check_archive([str(apk)], root))
        apk.unlink()

        # 12. Archive containing a forbidden website file.
        apk2 = root / "test2.apk"
        with zipfile.ZipFile(apk2, "w") as zf:
            zf.writestr("assets/index.html", b"<html>")
        expect_failure("archive forbidden name", lambda: check_archive([str(apk2)], root))
        apk2.unlink()

        # 13. Clean archive passes.
        apk3 = root / "test3.apk"
        with zipfile.ZipFile(apk3, "w") as zf:
            zf.writestr("classes.dex", b"DEX")
            zf.writestr("res/drawable/app_icon.png", b"APP-ICON-BYTES")
        expect_success("clean archive", lambda: check_archive([str(apk3)], root))
        apk3.unlink()

    if failures:
        print("SELF-TEST FAILED:")
        for f in failures:
            print(f"  - {f}")
        return 1
    print("OK: all self-test fixtures behaved as expected.")
    return 0


def main(argv) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    command = argv[1]
    if command == "check-source":
        return check_source()
    if command == "check-pages":
        return check_pages()
    if command == "check-archive":
        return check_archive(argv[2:])
    if command == "self-test":
        return self_test()
    print(f"unknown command: {command}", file=sys.stderr)
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
