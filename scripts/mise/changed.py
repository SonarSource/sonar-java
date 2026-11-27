import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

from tasks import ROOT, custom_rules_example, licenses, maven, unit


POM_FILE = "pom.xml"
MAVEN_CONFIG = ".mvn/"
CHECK_CHANGED = "check:changed"
SKIP_HEADERS = "-Dlicense.skip=true"
EXAMPLE = "docs/java-custom-rules-example/"
CHECKS = ("java-checks", "java-checks-aws")
JVM_MODULE = "java-checks-test-sources"
MAVEN_NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)


def changed_paths():
    ref = os.environ.get("MISE_CHANGE_BASE", "origin/master")
    try:
        base = git("merge-base", "--", "HEAD", ref).decode().strip()
    except subprocess.CalledProcessError:
        sys.exit("No comparison base. Set MISE_CHANGE_BASE to an available target branch or commit.")
    tracked = git("diff", "--name-only", "--no-renames", "-z", base, "--")
    others = git("ls-files", "--others", "--exclude-standard", "-z")
    return set(os.fsdecode(tracked + others).rstrip("\0").split("\0")) - {""}


def reactor_modules(path=ROOT):
    pom = ET.parse(path / POM_FILE)
    for entry in pom.findall("m:modules/m:module", MAVEN_NS):
        module = path / entry.text.strip()
        yield module.relative_to(ROOT).as_posix()
        yield from reactor_modules(module)


def license_input(path):
    return (Path(path).name.startswith("pom") and path.endswith(".xml")) or path.startswith(
        (MAVEN_CONFIG, "sonar-java-plugin/license-resources/", "sonar-java-plugin/src/main/resources/licenses/")
    ) or Path(path).name in {"missing-dep-licenses.properties", "LICENSE.txt", "NOTICE.txt"}


def example_input(path):
    return path.startswith((EXAMPLE, MAVEN_CONFIG)) or path == POM_FILE


def java_input(path):
    return "/src/" in path or path.startswith("java-checks/src/test/files/") or Path(path).name == POM_FILE


def affected_modules(paths):
    if any(path == POM_FILE or path.startswith(MAVEN_CONFIG) or path == "forbid_junit4.txt" for path in paths):
        return None
    modules = sorted(reactor_modules(), key=len, reverse=True)
    return {module for path in paths if java_input(path) and not path.startswith(EXAMPLE)
            for module in [next((m for m in modules if path.startswith(f"{m}/")), None)] if module}


def existing_test(module, test):
    return next((ROOT / module / "src/test/java").rglob(f"{test}.java"), None) is not None


def test_candidates(path, modules):
    stem = Path(path).stem
    if path.startswith(f"{JVM_MODULE}/"):
        sample = re.fullmatch(r"(\w+Check)Sample(?:_?Java\d+)?", stem)
        return [(module, f"{sample[1]}Test") for module in CHECKS] if sample else []
    if "/src/test/java/" in path and stem.endswith("Test"):
        return [(module, stem) for module in modules if path.startswith(f"{module}/")]
    if "/src/main/java/" in path and stem.endswith("Check"):
        return [(module, f"{stem}Test") for module in CHECKS if path.startswith(f"{module}/")]
    return []


def focused_tests(paths, modules):
    tests = set()
    for path in paths:
        if not java_input(path) or path.startswith(EXAMPLE):
            continue
        matches = {(module, test) for module, test in test_candidates(path, modules) if existing_test(module, test)}
        if not matches:
            return None
        tests.update(matches)
    return tests


def build_changed(modules):
    args = ["compile", "-Dsonar.skip=true"]
    if modules:
        args += ["-pl", ",".join(sorted(modules)), "-am", SKIP_HEADERS]
    maven(*args)


def test_changed(paths, modules):
    if not modules:
        unit(goal="test")
    elif tests := focused_tests(paths, modules):
        unit(",".join(sorted({module for module, _ in tests})), ",".join(sorted({test for _, test in tests})),
             goal="test", extra_args=(SKIP_HEADERS,))
    else:
        fixture_changes = any(module.startswith(JVM_MODULE) for module in modules)
        selection = None if fixture_changes else ",".join(sorted(modules))
        unit(selection, goal="test")


def run_changed(task, paths):
    if task in ("build:changed", "test:changed", CHECK_CHANGED):
        modules = affected_modules(paths)
        if modules == set():
            print("No changed reactor inputs outside the custom rules example.", flush=True)
        elif task == "build:changed":
            build_changed(modules)
        else:
            test_changed(paths, modules)
    if task in ("lint:licenses:changed", CHECK_CHANGED):
        if any(license_input(path) for path in paths):
            licenses()
        else:
            print("No changed license inputs.", flush=True)
    if task in ("test:custom-rules-example:changed", CHECK_CHANGED):
        if any(example_input(path) for path in paths):
            custom_rules_example()
        else:
            print("No changed custom rules example inputs.", flush=True)


if __name__ == "__main__":
    try:
        run_changed(sys.argv[1], changed_paths())
    except subprocess.CalledProcessError as error:
        sys.exit(error.returncode)
    except FileNotFoundError as error:
        sys.exit(f"Missing file or executable: {error.filename}")
