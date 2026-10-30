#!/usr/bin/env python3
"""Dump the ECJ frontend and the SemSitter frontend, then score AST and semantic mismatches.

The ECJ checkout is master (or any checkout that still parses with ECJ). The SemSitter checkout
is this branch. Both must already have java-frontend/target/classes.

    benchmark/compare.py --ecj ~/Documents/sonar-java
    benchmark/compare.py --ecj ~/Documents/sonar-java --corpus path/to/java --out benchmark/out
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
DUMP_SRC = Path(__file__).resolve().parent / "src" / "AstSemanticDump.java"


def git_sha(checkout: Path) -> str:
    return subprocess.check_output(["git", "-C", str(checkout), "rev-parse", "HEAD"], text=True).strip()


def move_develocity(checkout: Path):
    extensions = checkout / ".mvn" / "extensions.xml"
    backup = checkout / ".mvn" / "extensions.xml.bench-bak"
    if extensions.exists() and not backup.exists():
        extensions.rename(backup)
        return extensions, backup
    return None, None


def restore_develocity(extensions, backup):
    if extensions is not None and backup is not None and backup.exists() and not extensions.exists():
        backup.rename(extensions)


def tool_classpath(checkout: Path) -> str:
    classes = checkout / "java-frontend" / "target" / "classes"
    if not (classes / "org" / "sonar" / "java" / "model" / "JParser.class").exists():
        raise SystemExit(f"missing {classes}. Compile java-frontend in {checkout} first.")
    output = checkout / "java-frontend" / "target" / "bench-classpath.txt"
    extensions, backup = move_develocity(checkout)
    try:
        subprocess.run(
            [
                "mvn",
                "-pl",
                "java-frontend",
                "-DincludeScope=test",
                "-DexcludeArtifactIds=test-classpath-reader",
                "-DskipTests",
                "-q",
                "dependency:build-classpath",
                f"-Dmdep.outputFile={output}",
            ],
            cwd=checkout,
            check=True,
        )
    finally:
        restore_develocity(extensions, backup)
    jars = output.read_text().strip()
    pieces = [str(classes)]
    if jars:
        pieces.append(jars)
    return os.pathsep.join(pieces)


def java_files(corpus: Path) -> list[Path]:
    files = sorted(path for path in corpus.rglob("*.java") if path.is_file())
    if not files:
        raise SystemExit(f"no Java files under {corpus}")
    return files


def class_major(checkout: Path) -> int:
    classes = checkout / "java-frontend" / "target" / "classes"
    majors = []
    for relative in (
        "org/sonar/java/model/JParser.class",
        "org/sonar/java/semsitter/PlatformDetection.class",
    ):
        path = classes / relative
        if path.exists():
            majors.append(int.from_bytes(path.read_bytes()[6:8], "big"))
    if not majors:
        raise SystemExit(f"missing compiled frontend classes in {classes}")
    return max(majors)


def java_tools(major: int) -> tuple[str, str]:
    bindirs = []
    if major >= 69:
        bindirs.append(Path.home() / ".local/share/mise/installs/java/corretto-25/bin")
    home = os.environ.get("JAVA_HOME")
    if home:
        bindirs.append(Path(home) / "bin")
    bindirs.append(Path.home() / ".local/share/mise/installs/java/corretto-25/bin")
    for bindir in bindirs:
        if (bindir / "java").exists() and (bindir / "javac").exists():
            return str(bindir / "java"), str(bindir / "javac")
    return "java", "javac"


def dump(checkout: Path, classpath: str, corpus: Path, files: list[Path], out: Path) -> None:
    java, javac = java_tools(class_major(checkout))
    build = checkout / "java-frontend" / "target" / "ast-semantic-dump"
    build.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [javac, "--release", "21", "-cp", classpath, "-d", str(build), str(DUMP_SRC)],
        check=True,
    )
    command = [
        java,
        "-cp",
        os.pathsep.join([str(build), classpath]),
        "org.sonar.java.benchmark.AstSemanticDump",
        "--out",
        str(out),
        "--root",
        str(corpus),
    ]
    command.extend(str(path) for path in files)
    subprocess.run(command, check=True)


def load(path: Path) -> tuple[dict[str, dict[str, dict]], dict[str, str]]:
    nodes: dict[str, dict[str, dict]] = defaultdict(dict)
    errors: dict[str, str] = {}
    for line in path.read_text().splitlines():
        if not line.strip():
            continue
        record = json.loads(line)
        name = record["file"]
        if "error" in record:
            errors[name] = record["error"]
        else:
            nodes[name][record["path"]] = record
    return nodes, errors


def ancestors(path: str):
    parts = path.split("/")
    for index in range(1, len(parts)):
        yield "/".join(parts[:index])


def collapsed(paths: set[str]) -> list[str]:
    kept = []
    for path in sorted(paths):
        if any(parent in paths for parent in ancestors(path)):
            continue
        kept.append(path)
    return kept


def shown(record: dict, key: str):
    full = key + "Full"
    if full in record:
        return record[full]
    return record.get(key)


def semantic_kind(ecj: dict, sem: dict) -> str | None:
    symbol = "symbol" in ecj or "symbol" in sem
    types = "type" in ecj or "type" in sem
    if not symbol and not types:
        return None
    if symbol and ecj.get("symbol") != sem.get("symbol"):
        return "symbol"
    if types and ecj.get("type") != sem.get("type"):
        return "type"
    if shown(ecj, "symbol") != shown(sem, "symbol") or shown(ecj, "type") != shown(sem, "type"):
        return "type_arguments"
    return None


def span_differs(ecj: dict, sem: dict) -> bool:
    return any(ecj.get(key) != sem.get(key) for key in ("sl", "sc", "el", "ec"))


def show(record: dict | None) -> str:
    if record is None:
        return "absent"
    bits = [record.get("kind", "?")]
    if "text" in record:
        bits.append(json.dumps(record["text"]))
    if all(key in record for key in ("sl", "sc", "el", "ec")):
        bits.append(f"{record['sl']}:{record['sc']}-{record['el']}:{record['ec']}")
    if "symbol" in record:
        bits.append("symbol " + record.get("symbolFull", record["symbol"]))
    if "type" in record:
        bits.append("type " + record.get("typeFull", record["type"]))
    return " ".join(bits)


def compare(ecj_path: Path, sem_path: Path, summary_path: Path, text_path: Path, jsonl_path: Path, meta: dict) -> dict:
    ecj_nodes, ecj_errors = load(ecj_path)
    sem_nodes, sem_errors = load(sem_path)
    files = sorted(set(ecj_nodes) | set(sem_nodes) | set(ecj_errors) | set(sem_errors))
    only_ecj_roots: list[tuple[str, str]] = []
    only_sem_roots: list[tuple[str, str]] = []
    only_ecj_all: list[tuple[str, str]] = []
    only_sem_all: list[tuple[str, str]] = []
    spans: list[tuple[str, str]] = []
    semantic: list[tuple[str, str, str]] = []
    ecj_count = sem_count = shared = compared = 0
    for name in files:
        left = ecj_nodes.get(name, {})
        right = sem_nodes.get(name, {})
        ecj_count += len(left)
        sem_count += len(right)
        left_only = set(left) - set(right)
        right_only = set(right) - set(left)
        for path in sorted(left_only):
            only_ecj_all.append((name, path))
        for path in sorted(right_only):
            only_sem_all.append((name, path))
        for path in collapsed(left_only):
            only_ecj_roots.append((name, path))
        for path in collapsed(right_only):
            only_sem_roots.append((name, path))
        for path in sorted(set(left) & set(right)):
            shared += 1
            if span_differs(left[path], right[path]):
                spans.append((name, path))
            kind = semantic_kind(left[path], right[path])
            carries = "symbol" in left[path] or "symbol" in right[path] or "type" in left[path] or "type" in right[path]
            if carries:
                compared += 1
            if kind:
                semantic.append((name, path, kind))
    symbol_n = sum(1 for item in semantic if item[2] == "symbol")
    type_n = sum(1 for item in semantic if item[2] == "type")
    args_n = sum(1 for item in semantic if item[2] == "type_arguments")
    ast_mismatches = len(only_ecj_all) + len(only_sem_all) + len(spans)
    union = ecj_count + len(only_sem_all)
    summary = {
        **meta,
        "files": len(files),
        "ast": {
            "ecj_nodes": ecj_count,
            "semsitter_nodes": sem_count,
            "shared": shared,
            "only_ecj": len(only_ecj_all),
            "only_semsitter": len(only_sem_all),
            "only_ecj_roots": len(only_ecj_roots),
            "only_semsitter_roots": len(only_sem_roots),
            "span_mismatches": len(spans),
            "mismatches": ast_mismatches,
            "mismatch_proportion": (ast_mismatches / union) if union else 0.0,
            "only_ecj_proportion_of_ecj_nodes": (len(only_ecj_all) / ecj_count) if ecj_count else 0.0,
            "only_semsitter_proportion_of_semsitter_nodes": (len(only_sem_all) / sem_count) if sem_count else 0.0,
            "span_proportion_of_shared": (len(spans) / shared) if shared else 0.0,
        },
        "semantic": {
            "compared": compared,
            "symbol_mismatches": symbol_n,
            "type_mismatches": type_n,
            "type_argument_mismatches": args_n,
            "mismatches": len(semantic),
            "mismatch_proportion": (len(semantic) / compared) if compared else 0.0,
        },
        "parse_failures": {"ecj": len(ecj_errors), "semsitter": len(sem_errors)},
    }
    summary_path.write_text(json.dumps(summary, indent=2) + "\n")
    lines = [render(summary)]
    lines.append("")
    lines.append("Parse failures")
    if not ecj_errors and not sem_errors:
        lines.append("  none")
    for name, message in sorted(ecj_errors.items()):
        lines.append(f"  ECJ       {name}: {message}")
    for name, message in sorted(sem_errors.items()):
        lines.append(f"  SemSitter {name}: {message}")
    lines.append("")
    lines.append("Mismatches")
    lines.append("AST gaps below are the outermost missing node. Every missing node is in mismatches.jsonl.")
    records = []
    for name, path in only_ecj_all:
        records.append({"category": "ast_only_ecj", "file": name, "path": path, "ecj": ecj_nodes[name][path]})
    for name, path in only_ecj_roots:
        lines.append("AST only in ECJ")
        lines.append(f"  {name} {path}")
        lines.append(f"  ECJ       {show(ecj_nodes[name][path])}")
    for name, path in only_sem_all:
        records.append({"category": "ast_only_semsitter", "file": name, "path": path, "semsitter": sem_nodes[name][path]})
    for name, path in only_sem_roots:
        lines.append("AST only in SemSitter")
        lines.append(f"  {name} {path}")
        lines.append(f"  SemSitter {show(sem_nodes[name][path])}")
    for name, path in spans:
        records.append({"category": "span", "file": name, "path": path, "ecj": ecj_nodes[name][path], "semsitter": sem_nodes[name][path]})
        lines.append("SPAN")
        lines.append(f"  {name} {path}")
        lines.append(f"  ECJ       {show(ecj_nodes[name][path])}")
        lines.append(f"  SemSitter {show(sem_nodes[name][path])}")
    for name, path, kind in semantic:
        records.append({"category": kind, "file": name, "path": path, "ecj": ecj_nodes[name][path], "semsitter": sem_nodes[name][path]})
        lines.append(kind.upper())
        lines.append(f"  {name} {path}")
        lines.append(f"  ECJ       {show(ecj_nodes[name][path])}")
        lines.append(f"  SemSitter {show(sem_nodes[name][path])}")
    text_path.write_text("\n".join(lines) + "\n")
    jsonl_path.write_text("".join(json.dumps(record) + "\n" for record in records))
    return summary


def pct(value: float) -> str:
    return f"{value * 100:.1f}%"


def render(summary: dict) -> str:
    ast = summary["ast"]
    sem = summary["semantic"]
    return "\n".join([
        "AST and semantic benchmark",
        f"ECJ        {summary['ecj']['sha'][:12]}  {summary['ecj']['checkout']}",
        f"SemSitter  {summary['semsitter']['sha'][:12]}  {summary['semsitter']['checkout']}",
        f"Files      {summary['files']}",
        "",
        "AST",
        f"  ECJ nodes                         {ast['ecj_nodes']}",
        f"  SemSitter nodes                   {ast['semsitter_nodes']}",
        f"  Shared paths                      {ast['shared']}",
        f"  Only in ECJ                       {ast['only_ecj']}  ({pct(ast['only_ecj_proportion_of_ecj_nodes'])} of ECJ nodes)",
        f"  Only in SemSitter                 {ast['only_semsitter']}  ({pct(ast['only_semsitter_proportion_of_semsitter_nodes'])} of SemSitter nodes)",
        f"  Span mismatches                   {ast['span_mismatches']}  ({pct(ast['span_proportion_of_shared'])} of shared paths)",
        f"  Mismatches                        {ast['mismatches']}  ({pct(ast['mismatch_proportion'])} of ECJ nodes plus SemSitter-only nodes)",
        "",
        "Semantic, on shared nodes that carry a symbol or a type",
        f"  Compared                          {sem['compared']}",
        f"  Symbol mismatches                 {sem['symbol_mismatches']}",
        f"  Type mismatches                   {sem['type_mismatches']}",
        f"  Type-argument mismatches          {sem['type_argument_mismatches']}",
        f"  Mismatches                        {sem['mismatches']}  ({pct(sem['mismatch_proportion'])} of compared nodes)",
        "",
        f"Parse failures   ECJ {summary['parse_failures']['ecj']}   SemSitter {summary['parse_failures']['semsitter']}",
    ])


def main() -> None:
    parser = argparse.ArgumentParser(description="Compare ECJ and SemSitter AST and semantic dumps")
    parser.add_argument("--ecj", type=Path, help="checkout of the ECJ frontend, default ../sonar-java")
    parser.add_argument("--semsitter", type=Path, default=REPO, help="checkout of this frontend")
    parser.add_argument("--corpus", type=Path, default=REPO / "benchmark" / "corpus")
    parser.add_argument("--out", type=Path, default=REPO / "benchmark" / "out")
    args = parser.parse_args()
    ecj = args.ecj
    if ecj is None:
        candidate = REPO.parent / "sonar-java"
        if not candidate.exists():
            raise SystemExit("pass --ecj, the ECJ checkout was not found at ../sonar-java")
        ecj = candidate
    corpus = args.corpus.resolve()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    files = java_files(corpus)
    print(f"classpath {ecj}", file=sys.stderr)
    ecj_cp = tool_classpath(ecj)
    print(f"classpath {args.semsitter}", file=sys.stderr)
    sem_cp = tool_classpath(args.semsitter.resolve())
    ecj_dump = out / "ecj.jsonl"
    sem_dump = out / "semsitter.jsonl"
    print(f"dump ECJ ({len(files)} files)", file=sys.stderr)
    dump(ecj.resolve(), ecj_cp, corpus, files, ecj_dump)
    print("dump SemSitter", file=sys.stderr)
    dump(args.semsitter.resolve(), sem_cp, corpus, files, sem_dump)
    summary = compare(
        ecj_dump,
        sem_dump,
        out / "summary.json",
        out / "mismatches.txt",
        out / "mismatches.jsonl",
        {
            "ecj": {"checkout": str(ecj.resolve()), "sha": git_sha(ecj)},
            "semsitter": {"checkout": str(args.semsitter.resolve()), "sha": git_sha(args.semsitter)},
            "corpus": str(corpus),
        },
    )
    print(render(summary))


if __name__ == "__main__":
    main()
