#!/usr/bin/env python3
"""Dump the ECJ frontend and the SemSitter frontend, then score AST and semantic mismatches.

The ECJ checkout is master (or any checkout that still parses with ECJ). The SemSitter checkout
is this branch. Both must already have java-frontend/target/classes.

    benchmark/compare.py --ecj ~/Documents/sonar-java
    benchmark/compare.py --ecj ~/Documents/sonar-java --corpus path/to/java --out benchmark/out

Per-file unified diffs are written to <out>/diffs/, mirroring the corpus path:
<out>/diffs/src/main/java/Example.java.diff. The minus side is ECJ, the plus side is SemSitter.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
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


def corpus_classpath(corpus: Path) -> str:
    """Classpath the rule tests analyze the corpus with: its compiled classes and test-classpath.txt."""
    entries = []
    classes = corpus / "target" / "classes"
    if classes.exists():
        entries.append(str(classes))
    listing = corpus / "target" / "test-classpath.txt"
    if listing.exists():
        repository = os.environ.get("M2_REPO") or str(Path.home() / ".m2" / "repository")
        for entry in listing.read_text().split(":"):
            entry = entry.strip()
            if entry:
                entries.append(entry.replace("M2_REPO", repository))
    return os.pathsep.join(entries)


def dump(checkout: Path, classpath: str, corpus: Path, files: list[Path], out: Path, corpus_cp: str) -> None:
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
        "--classpath",
        corpus_cp,
    ]
    command.extend(str(path) for path in files)
    subprocess.run(command, check=True)


def load(path: Path) -> tuple[dict[str, dict[str, dict]], dict[str, str]]:
    nodes: dict[str, dict[str, dict]] = defaultdict(dict)
    errors: dict[str, str] = {}
    for line in path.read_text().split("\n"):
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


def one_line(text: str) -> str:
    return (
        text.replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u0085", "\\u0085")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
    )


def position(record: dict | None) -> tuple[int, int]:
    if record is None:
        return (10**9, 10**9)
    return (record.get("sl", 10**9), record.get("sc", 10**9))


def write_diffs(
    directory: Path,
    ecj_nodes: dict[str, dict[str, dict]],
    sem_nodes: dict[str, dict[str, dict]],
    ecj_errors: dict[str, str],
    sem_errors: dict[str, str],
    only_ecj_roots: list[tuple[str, str]],
    only_sem_roots: list[tuple[str, str]],
    spans: list[tuple[str, str]],
    semantic: list[tuple[str, str, str]],
) -> int:
    if directory.exists():
        shutil.rmtree(directory)
    roots_ecj: dict[str, list[str]] = defaultdict(list)
    roots_sem: dict[str, list[str]] = defaultdict(list)
    span_paths: dict[str, list[str]] = defaultdict(list)
    semantic_paths: dict[str, list[tuple[str, str]]] = defaultdict(list)
    for name, path in only_ecj_roots:
        roots_ecj[name].append(path)
    for name, path in only_sem_roots:
        roots_sem[name].append(path)
    for name, path in spans:
        span_paths[name].append(path)
    for name, path, kind in semantic:
        semantic_paths[name].append((path, kind))
    names = sorted(
        set(roots_ecj)
        | set(roots_sem)
        | set(span_paths)
        | set(semantic_paths)
        | set(ecj_errors)
        | set(sem_errors)
    )
    written = 0
    for name in names:
        left = ecj_nodes.get(name, {})
        right = sem_nodes.get(name, {})
        text = file_diff(
            name,
            left,
            right,
            ecj_errors.get(name),
            sem_errors.get(name),
            roots_ecj.get(name, []),
            roots_sem.get(name, []),
            span_paths.get(name, []),
            semantic_paths.get(name, []),
        )
        if text is None:
            continue
        destination = directory / (name + ".diff")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(text)
        written += 1
    return written


def file_diff(
    name: str,
    left: dict[str, dict],
    right: dict[str, dict],
    ecj_error: str | None,
    sem_error: str | None,
    ecj_roots: list[str],
    sem_roots: list[str],
    span_paths: list[str],
    semantic_paths: list[tuple[str, str]],
) -> str | None:
    whole_file = (ecj_error is not None and not left) or (sem_error is not None and not right)
    hunks: list[tuple[tuple[int, int, str], list[str]]] = []
    if ecj_error is not None or sem_error is not None:
        left_status = ecj_error if ecj_error is not None else f"parsed, {len(left)} nodes"
        right_status = sem_error if sem_error is not None else f"parsed, {len(right)} nodes"
        hunks.append(((0, 0, ""), [
            "@@ -1 +1 @@ parse",
            "-" + one_line(left_status),
            "+" + one_line(right_status),
        ]))
    if not whole_file:
        for path in ecj_roots:
            record = left[path]
            hunks.append(((*position(record), path), [
                f"@@ -1 +0,0 @@ ast_only_ecj {path}",
                "-" + one_line(show(record)),
            ]))
        for path in sem_roots:
            record = right[path]
            hunks.append(((*position(record), path), [
                f"@@ -0,0 +1 @@ ast_only_semsitter {path}",
                "+" + one_line(show(record)),
            ]))
        for path in span_paths:
            hunks.append(((*position(left[path]), path), [
                f"@@ -1 +1 @@ span {path}",
                "-" + one_line(show(left[path])),
                "+" + one_line(show(right[path])),
            ]))
        for path, kind in semantic_paths:
            hunks.append(((*position(left[path]), path), [
                f"@@ -1 +1 @@ {kind} {path}",
                "-" + one_line(show(left[path])),
                "+" + one_line(show(right[path])),
            ]))
    if not hunks:
        return None
    hunks.sort(key=lambda item: item[0])
    ecj_only = sum(1 for path in left if path not in right) if not whole_file else 0
    sem_only = sum(1 for path in right if path not in left) if not whole_file else 0
    union = len(left) + sem_only
    ast_mismatches = ecj_only + sem_only + (0 if whole_file else len(span_paths))
    compared = 0
    if not whole_file:
        for path in set(left) & set(right):
            node = left[path]
            other = right[path]
            if "symbol" in node or "symbol" in other or "type" in node or "type" in other:
                compared += 1
    semantic_n = 0 if whole_file else len(semantic_paths)
    semantic_proportion = (semantic_n / compared) if compared else 0.0
    ast_proportion = (ast_mismatches / union) if union else 0.0
    if whole_file:
        ast_line = "# AST mismatches: one side did not parse this file"
    else:
        ast_line = f"# AST mismatches {ast_mismatches} ({pct(ast_proportion)} of ECJ nodes plus SemSitter-only nodes)"
    header = [
        f"# {name}",
        f"# ECJ nodes {len(left)}   SemSitter nodes {len(right)}",
        ast_line,
        f"# Semantic mismatches {semantic_n} of {compared} compared ({pct(semantic_proportion)})",
        "# AST hunks are the outermost missing node.",
        f"--- ecj/{name}",
        f"+++ semsitter/{name}",
    ]
    body = []
    for _, lines in hunks:
        body.extend(lines)
    return "\n".join(header + body) + "\n"


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
    diff_dir = summary_path.parent / "diffs"
    summary["diffs"] = str(diff_dir)
    summary["diff_files"] = write_diffs(
        diff_dir,
        ecj_nodes,
        sem_nodes,
        ecj_errors,
        sem_errors,
        only_ecj_roots,
        only_sem_roots,
        spans,
        semantic,
    )
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
        f"Diff files       {summary.get('diff_files', 0)}   {summary.get('diffs', '')}",
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
    corpus_cp = corpus_classpath(corpus)
    ecj_dump = out / "ecj.jsonl"
    sem_dump = out / "semsitter.jsonl"
    print(f"dump ECJ ({len(files)} files)", file=sys.stderr)
    dump(ecj.resolve(), ecj_cp, corpus, files, ecj_dump, corpus_cp)
    print("dump SemSitter", file=sys.stderr)
    dump(args.semsitter.resolve(), sem_cp, corpus, files, sem_dump, corpus_cp)
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
