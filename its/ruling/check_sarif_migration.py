"""Compare ruling SARIF issue locations with the JSON files removed by a PR."""

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import subprocess
import sys


EXPECTED_DIR = Path("its/ruling/src/test/resources/expected")
REPO_ROOT = Path(__file__).resolve().parents[2]


def changed_files(base):
    changes = subprocess.check_output(
        ["git", "diff", "--name-status", "-z", "--no-renames", base, "HEAD", "--", str(EXPECTED_DIR)],
        cwd=REPO_ROOT,
    ).decode().split("\0")
    added = set()
    removed = set()
    for status, path in zip(changes[0::2], changes[1::2]):
        if status == "A" and path.endswith(".sarif"):
            added.add(Path(path))
        elif status == "D" and path.endswith(".json"):
            removed.add(Path(path))
    return added, removed


def json_locations(base, path):
    data = json.loads(subprocess.check_output(
        ["git", "show", f"{base}:{path.as_posix()}"], cwd=REPO_ROOT
    ))
    return Counter((uri, line) for uri, lines in data.items() for line in lines)


def sarif_locations(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    expected_rule = path.stem.replace("-", ":", 1)
    locations = Counter()
    for run in data["runs"]:
        for result in run.get("results", []):
            if result["ruleId"] != expected_rule:
                raise ValueError(f"{path}: unexpected ruleId {result['ruleId']!r}")
            for location in result["locations"]:
                physical = location["physicalLocation"]
                uri = physical["artifactLocation"]["uri"]
                line = physical.get("region", {}).get("startLine", 0)
                locations[uri, line] += 1
    return locations


def print_locations(locations):
    for (uri, line), count in sorted(locations.items()):
        for _ in range(count):
            print(f"  {uri}:{line}")


def check_pair(stem, added, removed, base, repo_root, summary_only):
    sarif_path = stem.with_suffix(".sarif")
    json_path = stem.with_suffix(".json")
    print(f"{stem.relative_to(EXPECTED_DIR)}:")
    if json_path not in removed:
        print("  MISMATCH: no removed JSON counterpart")
        return False

    legacy = json_locations(base, json_path)
    if sarif_path not in added:
        if not legacy:
            print("  MATCH: 0 issue locations (empty JSON removed)")
            return True
        print(f"  MISMATCH: no added SARIF for {sum(legacy.values())} issue locations")
        return False

    sarif = sarif_locations(repo_root / sarif_path)
    if not summary_only:
        print_locations(sarif)
    missing = legacy - sarif
    extra = sarif - legacy
    if missing or extra:
        print(f"  MISMATCH: {sum(missing.values())} missing, {sum(extra.values())} extra")
        for label, locations in (("missing", missing), ("extra", extra)):
            for (uri, line), count in sorted(locations.items()):
                print(f"    {label}: {uri}:{line} ({count})")
        return False

    print(f"  MATCH: {sum(sarif.values())} issue locations")
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="origin/master", help="branch to compare with (default: origin/master)")
    parser.add_argument("--summary-only", action="store_true", help="omit individual SARIF locations")
    args = parser.parse_args()

    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_./-]*", args.base):
        parser.error("--base must be a Git branch or commit name")
    base = subprocess.check_output(
        ["git", "merge-base", "HEAD", args.base], cwd=REPO_ROOT
    ).decode().strip()
    added, removed = changed_files(base)
    pairs = sorted({path.with_suffix("") for path in added | removed})
    mismatches = sum(
        not check_pair(stem, added, removed, base, REPO_ROOT, args.summary_only)
        for stem in pairs
    )

    print(f"Checked {len(pairs)} project/rule pairs; {mismatches} mismatches")
    return 1 if mismatches else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, subprocess.CalledProcessError, ValueError, KeyError, TypeError) as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(2)
