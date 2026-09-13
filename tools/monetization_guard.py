#!/usr/bin/env python3
"""Guard against the optional monetization stack leaking into the shipping app.

The RevenueCat + Firebase monetization feature lives behind the Gradle property
`shweep.monetization.enabled` and the isolated `:monetization` module. With the
default value (`false`) the shipping app must contain no RevenueCat/Firebase
code, dependencies, configuration, keys, or network activity.

Usage:
  python3 tools/monetization_guard.py check-source
  python3 tools/monetization_guard.py check-config
  python3 tools/monetization_guard.py check-archive <path> [<path> ...]
  python3 tools/monetization_guard.py check-dependencies-file <file>
  python3 tools/monetization_guard.py self-test
"""

import os
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]

# Directories whose source must stay free of the monetization SDKs. The
# :monetization module itself is the isolated home for those SDKs, so it is
# intentionally not part of this scan.
SOURCE_SCAN_DIRS = ("composeApp/src", "iosApp/iosApp")

# Build files that must not pull monetization dependencies into the app.
APP_BUILD_FILES = ("build.gradle.kts", "composeApp/build.gradle.kts")

# Source-level tokens that indicate RevenueCat / Firebase usage.
FORBIDDEN_SOURCE_TOKENS = (
    "com.revenuecat",
    "com.google.firebase",
    "com.google.android.gms.auth",
    "Purchases.configure",
    "Purchases.sharedInstance",
    "RevenueCat",
    "FirebaseAuth",
    "FirebaseFirestore",
    "FirebaseFunctions",
    "GoogleSignIn",
)

# Gradle dependency coordinates that must not enter the app build.
FORBIDDEN_DEPENDENCY_TOKENS = (
    "com.revenuecat",
    "com.google.firebase",
    "com.google.gms:google-services",
    "com.google.android.gms:play-services-auth",
    "purchases-kmp",
)

# Firebase/RevenueCat configuration files that must not be committed.
FORBIDDEN_CONFIG_NAMES = ("google-services.json", "GoogleService-Info.plist")

# Credential material that must never appear in the repository.
SECRET_PATTERNS = (
    re.compile(r"\bsk_[A-Za-z0-9]{8,}"),          # RevenueCat secret key
    re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    re.compile(r"\bAIza[0-9A-Za-z_\-]{20,}"),      # Google API key
)

TEXT_EXTENSIONS = {
    ".kt", ".kts", ".swift", ".json", ".plist", ".md", ".ts", ".js",
    ".toml", ".properties", ".yml", ".yaml", ".xml",
}

SKIP_DIRS = {".git", ".gradle", "build", ".idea", "node_modules", ".opencode"}

MONETIZATION_PROPERTY = "shweep.monetization.enabled"
DEFAULT_PROPERTY = "shweep.monetization.enabled=false"


def _rel(root: Path, path) -> str:
    try:
        return str(Path(path).relative_to(root))
    except ValueError:
        return str(path)


def _committed_files(root: Path):
    """Return git-tracked paths, or None when not a git checkout."""
    if not (root / ".git").exists():
        return None
    try:
        out = subprocess.run(
            ["git", "-C", str(root), "ls-files"],
            capture_output=True, text=True, check=True,
        )
    except Exception:
        return None
    return [line for line in out.stdout.splitlines() if line]


def _iter_text_files(root: Path, base: Path):
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            path = Path(dirpath) / name
            if path.suffix.lower() in TEXT_EXTENSIONS:
                yield path


def _report(violations, ok_message: str) -> int:
    if violations:
        print("ERROR: monetization boundary violations detected:")
        for v in sorted(set(violations)):
            print(f"  - {v}")
        return 1
    print(ok_message)
    return 0


def check_source(root: Path = REPO_ROOT) -> int:
    violations = []

    for rel_dir in SOURCE_SCAN_DIRS:
        base = root / rel_dir
        if not base.is_dir():
            continue
        for path in _iter_text_files(root, base):
            try:
                text = path.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            for token in FORBIDDEN_SOURCE_TOKENS:
                if token in text:
                    violations.append(
                        f"monetization token '{token}' in {_rel(root, path)}"
                    )

    for rel in APP_BUILD_FILES:
        path = root / rel
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for token in FORBIDDEN_DEPENDENCY_TOKENS:
            if token in text:
                violations.append(
                    f"monetization dependency '{token}' in {_rel(root, path)}"
                )

    # No committed Firebase/RevenueCat configuration.
    committed = _committed_files(root)
    if committed is not None:
        for name in FORBIDDEN_CONFIG_NAMES:
            for tracked in committed:
                if Path(tracked).name == name:
                    violations.append(
                        f"committed monetization config file: {tracked}"
                    )

    # No credential material in tracked text files.
    if committed is not None:
        for tracked in committed:
            path = root / tracked
            if not path.is_file() or path.suffix.lower() not in TEXT_EXTENSIONS:
                continue
            try:
                text = path.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            for pattern in SECRET_PATTERNS:
                if pattern.search(text):
                    violations.append(
                        f"possible embedded credential in {tracked}"
                    )

    violations.extend(_config_violations(root))

    return _report(
        violations,
        "OK: no RevenueCat/Firebase code, dependencies, config, or secrets in the app.",
    )


def _config_violations(root: Path):
    """Verify the monetization module stays flag-gated and opt-in."""
    violations = []

    settings = root / "settings.gradle.kts"
    if not settings.is_file():
        violations.append("missing settings.gradle.kts")
        return violations

    text = settings.read_text(encoding="utf-8", errors="replace")
    include_index = text.find('include(":monetization")')
    property_index = text.find(MONETIZATION_PROPERTY)

    if include_index == -1:
        violations.append("settings.gradle.kts does not declare :monetization")
    elif property_index == -1 or property_index > include_index:
        violations.append(
            "settings.gradle.kts includes :monetization without gating it behind "
            f"{MONETIZATION_PROPERTY}"
        )

    properties = root / "gradle.properties"
    if not properties.is_file():
        violations.append("missing gradle.properties")
    else:
        prop_text = properties.read_text(encoding="utf-8", errors="replace")
        if DEFAULT_PROPERTY not in prop_text:
            violations.append(
                f"gradle.properties must default to {DEFAULT_PROPERTY}"
            )

    return violations


def check_config(root: Path = REPO_ROOT) -> int:
    return _report(
        _config_violations(root),
        "OK: monetization module is opt-in and disabled by default.",
    )


def _check_zip(archive: Path) -> list:
    violations = []
    with zipfile.ZipFile(archive) as zf:
        for info in zf.infolist():
            name = info.filename
            base = os.path.basename(name)
            if base in FORBIDDEN_CONFIG_NAMES:
                violations.append(f"{archive}: bundled config file {name}")
                continue
            lower = name.lower()
            if "revenuecat" in lower or "google-services" in lower:
                violations.append(f"{archive}: monetization entry {name}")
                continue
            if "firebase" in lower:
                violations.append(f"{archive}: firebase entry {name}")
                continue
            if name.endswith(".dex") or name.endswith(".so"):
                data = zf.read(name)
                if _contains_monetization_bytes(data):
                    violations.append(f"{archive}: monetization code inside {name}")
    return violations


def _check_bundle(bundle: Path) -> list:
    violations = []
    for dirpath, dirnames, filenames in os.walk(bundle):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            path = Path(dirpath) / name
            if name in FORBIDDEN_CONFIG_NAMES:
                violations.append(f"{bundle}: bundled config file {path}")
                continue
            lower = name.lower()
            if "revenuecat" in lower or "firebase" in lower:
                violations.append(f"{bundle}: monetization file {path}")
                continue
            if path.suffix in {".dylib", ""} or name == "Shweep":
                try:
                    data = path.read_bytes()
                except OSError:
                    continue
                if _contains_monetization_bytes(data):
                    violations.append(f"{bundle}: monetization symbols in {path}")
    return violations


def _contains_monetization_bytes(data: bytes) -> bool:
    if not data:
        return False
    for needle in (b"com/revenuecat", b"com.google.firebase", b"RevenueCat"):
        if needle in data:
            return True
    return False


def check_archive(paths, root: Path = REPO_ROOT) -> int:
    if not paths:
        print("error: provide at least one archive or bundle path", file=sys.stderr)
        return 2

    violations = []
    for path in paths:
        if not os.path.exists(path):
            print(f"error: path not found: {path}", file=sys.stderr)
            return 2
        if os.path.isdir(path):
            violations.extend(_check_bundle(Path(path)))
        else:
            violations.extend(_check_zip(Path(path)))

    return _report(violations, "OK: no monetization artifacts in packaged output.")


def check_dependencies_file(paths, root: Path = REPO_ROOT) -> int:
    if not paths:
        print("error: provide a dependencies dump file", file=sys.stderr)
        return 2
    violations = []
    for path in paths:
        file = Path(path)
        if not file.is_file():
            print(f"error: file not found: {path}", file=sys.stderr)
            return 2
        text = file.read_text(encoding="utf-8", errors="replace")
        for token in FORBIDDEN_DEPENDENCY_TOKENS:
            if token in text:
                violations.append(f"{path} contains dependency '{token}'")
    return _report(
        violations,
        "OK: dependency graph contains no RevenueCat/Firebase.",
    )


def self_test() -> int:
    failures = []

    def expect_failure(name, fn):
        try:
            code = fn()
        except Exception as exc:  # noqa: BLE001
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
        (root / "composeApp/src/commonMain/kotlin").mkdir(parents=True)
        (root / "iosApp/iosApp").mkdir(parents=True)
        (root / "monetization/src/commonMain/kotlin").mkdir(parents=True)
        (root / "gradle.properties").write_text(DEFAULT_PROPERTY + "\n")
        (root / "settings.gradle.kts").write_text(
            "val enabled = providers.gradleProperty(\""
            + MONETIZATION_PROPERTY
            + "\")\nif (enabled) {\n    include(\":monetization\")\n}\n"
        )
        (root / "composeApp/build.gradle.kts").write_text("dependencies { }\n")
        (root / "build.gradle.kts").write_text("plugins { }\n")

        expect_success("clean source", lambda: check_source(root))
        expect_success("clean config", lambda: check_config(root))

        # Monetization token in app source.
        app_file = root / "composeApp/src/commonMain/kotlin/App.kt"
        app_file.write_text("import com.google.firebase.auth.FirebaseAuth\n")
        expect_failure("source token", lambda: check_source(root))
        app_file.write_text("import com.revenuecat.purchases.Purchases\n")
        expect_failure("revenuecat source token", lambda: check_source(root))
        app_file.unlink()

        # Monetization dependency in app build file.
        build = root / "composeApp/build.gradle.kts"
        build.write_text('implementation("com.revenuecat.purchases:purchases-kmp-core")\n')
        expect_failure("app dependency", lambda: check_source(root))
        build.write_text("dependencies { }\n")

        # Property default removed.
        (root / "gradle.properties").write_text("shweep.monetization.enabled=true\n")
        expect_failure("property default", lambda: check_config(root))
        (root / "gradle.properties").write_text(DEFAULT_PROPERTY + "\n")

        # Module included without gating.
        (root / "settings.gradle.kts").write_text('include(":monetization")\n')
        expect_failure("ungated include", lambda: check_config(root))
        (root / "settings.gradle.kts").write_text(
            "val enabled = providers.gradleProperty(\""
            + MONETIZATION_PROPERTY
            + "\")\nif (enabled) {\n    include(\":monetization\")\n}\n"
        )

        # Committed Firebase config file.
        subprocess.run(["git", "-C", str(root), "init", "-q"], check=False)
        subprocess.run(["git", "-C", str(root), "config", "user.email", "t@t"], check=False)
        subprocess.run(["git", "-C", str(root), "config", "user.name", "t"], check=False)
        (root / "composeApp/google-services.json").write_text("{}\n")
        subprocess.run(["git", "-C", str(root), "add", "-f", "composeApp/google-services.json"], check=False)
        expect_failure("committed config", lambda: check_source(root))

        # Archive with a monetization entry.
        apk = root / "test.apk"
        with zipfile.ZipFile(apk, "w") as zf:
            zf.writestr("classes.dex", b"DEX")
            zf.writestr("assets/GoogleService-Info.plist", b"<plist/>")
        expect_failure("archive config", lambda: check_archive([str(apk)], root))
        apk.unlink()

        # Archive with embedded monetization code.
        apk2 = root / "test2.apk"
        with zipfile.ZipFile(apk2, "w") as zf:
            zf.writestr("classes.dex", b"Lcom/revenuecat/purchases/Purchases;")
        expect_failure("archive code", lambda: check_archive([str(apk2)], root))
        apk2.unlink()

        # Clean archive passes.
        apk3 = root / "test3.apk"
        with zipfile.ZipFile(apk3, "w") as zf:
            zf.writestr("classes.dex", b"Lcom/example/App;")
        expect_success("clean archive", lambda: check_archive([str(apk3)], root))
        apk3.unlink()

        # Dependency dump file.
        deps = root / "deps.txt"
        deps.write_text("+--- com.revenuecat.purchases:purchases-kmp-core:3.8.0\n")
        expect_failure("dependency dump", lambda: check_dependencies_file([str(deps)], root))
        deps.write_text("+--- androidx.datastore:datastore-core:1.1.1\n")
        expect_success("clean dependency dump", lambda: check_dependencies_file([str(deps)], root))

    if failures:
        print("SELF-TEST FAILED:")
        for f in failures:
            print(f"  - {f}")
        return 1
    print("OK: all monetization self-test fixtures behaved as expected.")
    return 0


def main(argv) -> int:
    if len(argv) < 2:
        print(__doc__, file=sys.stderr)
        return 2
    command = argv[1]
    if command == "check-source":
        return check_source()
    if command == "check-config":
        return check_config()
    if command == "check-archive":
        return check_archive(argv[2:])
    if command == "check-dependencies-file":
        return check_dependencies_file(argv[2:])
    if command == "self-test":
        return self_test()
    print(f"unknown command: {command}", file=sys.stderr)
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
