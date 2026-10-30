#!/usr/bin/env python3
"""Sample AST mismatches and semantic mismatches separately, 1000 each.

    python3 benchmark/bucket_sample.py

Writes ast-issue-buckets.csv and semantic-issue-buckets.csv. A file with a handful
of mismatches and a file with millions contribute the same number of rows, until a
file runs out. The fix column says where that bucket has to change: bridge (this
repo), semsitter (resolution in SemSitter), or not a fix.
"""

from __future__ import annotations

import argparse
import csv
import importlib.util
from collections import defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]


def compare_module():
    spec = importlib.util.spec_from_file_location("compare", Path(__file__).resolve().parent / "compare.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def parse_symbol(symbol: str | None):
    if symbol is None:
        return {"kind": "absent"}
    if symbol == "" or symbol == "unknown":
        return {"kind": "unknown"}
    if "#" not in symbol:
        return {"kind": "name", "name": symbol}
    owner, leaf = symbol.rsplit("#", 1)
    if "(" in leaf and leaf.endswith(")"):
        name, params = leaf[:-1].split("(", 1)
        return {
            "kind": "method",
            "owner": owner,
            "name": name,
            "params": [] if params == "" else params.split(","),
        }
    return {"kind": "member", "owner": owner, "name": leaf}


def type_role(value: str | None) -> str:
    if value is None:
        return "absent"
    if value == "unknown":
        return "unknown"
    if value in {"void", "int", "boolean", "byte", "short", "char", "long", "float", "double"}:
        return "primitive"
    if value.endswith("[]"):
        return "array"
    return "named"


def symbol_issue(ecj: dict, sem: dict) -> str:
    left = parse_symbol(ecj.get("symbol"))
    right = parse_symbol(sem.get("symbol"))
    if left["kind"] == "unknown" and right["kind"] == "method":
        return "SemSitter resolves a method ECJ leaves unknown"
    if left["kind"] == "method" and right["kind"] == "unknown":
        return "ECJ resolves a method SemSitter leaves unknown"
    if left["kind"] == "unknown" and right["kind"] == "member":
        return "SemSitter resolves a field or variable ECJ leaves unknown"
    if left["kind"] == "member" and right["kind"] == "unknown":
        return "ECJ resolves a field or variable SemSitter leaves unknown"
    if left["kind"] == "unknown" and right["kind"] == "name":
        return "SemSitter resolves a type or package ECJ leaves unknown"
    if left["kind"] == "name" and right["kind"] == "unknown":
        return "ECJ resolves a type or package SemSitter leaves unknown"
    if {left["kind"], right["kind"]} == {"method", "member"}:
        return "one side resolves a method, the other a field or variable"
    if {left["kind"], right["kind"]} == {"method", "name"}:
        return "one side resolves a method, the other a type or package"
    if {left["kind"], right["kind"]} == {"member", "name"}:
        return "one side resolves a field or variable, the other a type or package"
    if left["kind"] == "method" and right["kind"] == "method":
        return method_issue(left, right)
    if left["kind"] == "member" and right["kind"] == "member":
        if "$lambda" in left["owner"] or "$lambda" in right["owner"]:
            return "lambda variable owner differs"
        if left["name"] != right["name"]:
            return "field or variable name differs"
        return "field or variable owner differs"
    if left["kind"] == "name" and right["kind"] == "name":
        return "type or package name differs"
    return "symbol differs"


def method_issue(left: dict, right: dict) -> str:
    if left["owner"] == "" and right["owner"] != "":
        return "method receiver is missing on ECJ"
    if right["owner"] == "" and left["owner"] != "":
        return "method receiver is missing on SemSitter"
    if left["name"] != right["name"]:
        return "method name differs"
    left_unknown = "unknown" in left["params"]
    right_unknown = "unknown" in right["params"]
    if right_unknown and not left_unknown:
        return "method parameter type is unknown on SemSitter"
    if left_unknown and not right_unknown:
        return "method parameter type is unknown on ECJ"
    if left_unknown and right_unknown and left["params"] != right["params"]:
        return "method parameter types are unknown on both sides, in different places"
    if len(left["params"]) != len(right["params"]):
        return "method arity differs"
    if left["owner"] != right["owner"]:
        return "method resolves to a different owner"
    if left["params"] != right["params"]:
        return "method parameter types differ"
    return "method symbol differs"


def type_issue(ecj: dict, sem: dict) -> str:
    left = type_role(ecj.get("type"))
    right = type_role(sem.get("type"))
    if right == "unknown" and left != "unknown":
        kind = parse_symbol(ecj.get("symbol") or sem.get("symbol"))["kind"]
        if kind == "method":
            return "method return type is unknown"
        if kind == "member":
            return "field or variable type is unknown"
        return "expression type is unknown"
    if left == "unknown" and right != "unknown":
        return "type is ascribed where ECJ leaves it unknown"
    if (ecj.get("type") or "").count("[]") != (sem.get("type") or "").count("[]"):
        return "array type rank differs"
    return "resolved types differ"


BRIDGE = {
    "method return type is unknown",
    "expression type is unknown",
    "type is ascribed where ECJ leaves it unknown",
    "array type rank differs",
    "ECJ resolves a type or package SemSitter leaves unknown",
    "SemSitter resolves a type or package ECJ leaves unknown",
    "one side resolves a method, the other a field or variable",
    "parse failure on SemSitter",
    "parse failure on both sides",
}

NOT_A_FIX = {
    "method parameter type is unknown on ECJ",
    "parse failure on ECJ",
}


def fix_for(name: str, category: str) -> str:
    if name in NOT_A_FIX:
        return "not a fix"
    if name in BRIDGE or category in {"span", "ast_only_ecj", "ast_only_semsitter", "parse"}:
        return "bridge"
    return "semsitter"


def span_issue(ecj: dict, sem: dict) -> str:
    edges = [key for key in ("sl", "sc", "el", "ec") if ecj.get(key) != sem.get(key)]
    if (
        ecj.get("kind") == "COMPILATION_UNIT"
        and edges == ["el", "ec"]
        and sem.get("el") == ecj.get("el", 0) - 1
        and ecj.get("ec") == 0
        and sem.get("ec") == 1
    ):
        return "compilation unit ends on the previous line, at column 1"
    joined = "+".join(edges)
    if joined == "ec":
        return "end column differs"
    if joined == "el+ec":
        return "end position differs"
    if joined == "sc":
        return "start column differs"
    if joined == "sl+sc+el+ec":
        return "whole span differs"
    return f"span differs in {joined}"


def ast_issue(category: str, record: dict) -> str:
    kind = record.get("kind", "?")
    if category == "ast_only_ecj":
        return f"node missing on SemSitter: {kind}"
    return f"node only on SemSitter: {kind}"


def issue_name(item: dict) -> str:
    category = item["category"]
    if category == "parse":
        if item.get("ecj_error") and item.get("sem_error"):
            return "parse failure on both sides"
        if item.get("sem_error"):
            return "parse failure on SemSitter"
        return "parse failure on ECJ"
    if category == "symbol":
        return symbol_issue(item["ecj"], item["sem"])
    if category == "type":
        return type_issue(item["ecj"], item["sem"])
    if category == "span":
        return span_issue(item["ecj"], item["sem"])
    node = item.get("ecj") or item.get("sem") or {}
    return ast_issue(category, node)


def collect(module, ecj_nodes, sem_nodes, ecj_errors, sem_errors) -> dict[str, list[dict]]:
    by_file: dict[str, list[dict]] = defaultdict(list)
    files = sorted(set(ecj_nodes) | set(sem_nodes) | set(ecj_errors) | set(sem_errors))
    for name in files:
        left = ecj_nodes.get(name, {})
        right = sem_nodes.get(name, {})
        if name in ecj_errors or name in sem_errors:
            by_file[name].append({
                "category": "parse",
                "file": name,
                "path": "",
                "ecj_error": ecj_errors.get(name),
                "sem_error": sem_errors.get(name),
                "line": 0,
            })
        if (name in ecj_errors and not left) or (name in sem_errors and not right):
            continue
        left_only = set(left) - set(right)
        right_only = set(right) - set(left)
        for path in module.collapsed(left_only):
            record = left[path]
            by_file[name].append(item("ast_only_ecj", name, path, record, None))
        for path in module.collapsed(right_only):
            record = right[path]
            by_file[name].append(item("ast_only_semsitter", name, path, None, record))
        for path in sorted(set(left) & set(right)):
            if module.span_differs(left[path], right[path]):
                by_file[name].append(item("span", name, path, left[path], right[path]))
            kind = module.semantic_kind(left[path], right[path])
            if kind:
                by_file[name].append(item(kind, name, path, left[path], right[path]))
    for name in by_file:
        by_file[name].sort(key=lambda row: (row["line"], row["path"], row["category"]))
    return by_file


def item(category: str, name: str, path: str, ecj: dict | None, sem: dict | None) -> dict:
    record = ecj or sem or {}
    return {
        "category": category,
        "file": name,
        "path": path,
        "ecj": ecj,
        "sem": sem,
        "line": record.get("sl", 0),
    }


def allocate(counts: dict[str, int], total: int) -> dict[str, int]:
    remaining = {name: count for name, count in counts.items() if count > 0}
    grant = {name: 0 for name in remaining}
    need = min(total, sum(remaining.values()))
    while need and remaining:
        share = need / len(remaining)
        poor = [name for name, count in remaining.items() if count <= share]
        if poor:
            for name in poor:
                taken = remaining.pop(name)
                grant[name] += taken
                need -= taken
            continue
        names = sorted(remaining)
        base, extra = divmod(need, len(names))
        extra_at = {int(i * len(names) / extra) for i in range(extra)} if extra else set()
        for index, name in enumerate(names):
            grant[name] += base + (1 if index in extra_at else 0)
        break
    return grant


def spaced(rows: list[dict], count: int) -> list[dict]:
    if count <= 0:
        return []
    if count >= len(rows):
        return list(rows)
    width = len(rows) // count
    offset = width // 2
    return [rows[index * width + offset] for index in range(count)]


def example_text(module, row: dict) -> str:
    if row["category"] == "parse":
        left = row.get("ecj_error") or "parsed"
        right = row.get("sem_error") or "parsed"
        return f"ECJ {left} | SemSitter {right}"
    return f"ECJ {module.show(row.get('ecj'))} | SemSitter {module.show(row.get('sem'))}"


def sample(by_file: dict[str, list[dict]], categories: set[str], total: int) -> list[dict]:
    selected = {}
    for name, rows in by_file.items():
        kept = [row for row in rows if row["category"] in categories]
        if kept:
            selected[name] = kept
    grant = allocate({name: len(rows) for name, rows in selected.items()}, total)
    sampled = []
    for name, rows in sorted(selected.items()):
        sampled.extend(spaced(rows, grant.get(name, 0)))
    return sampled


def write_csv(path: Path, module, sampled: list[dict]) -> int:
    groups: dict[str, list[dict]] = defaultdict(list)
    for row in sampled:
        groups[issue_name(row)].append(row)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["issue", "fix", "mismatch_count", "proportion", "category", "files", "example_file", "example"])
        for name, rows in sorted(groups.items(), key=lambda pair: (-len(pair[1]), pair[0])):
            categories = sorted({row["category"] for row in rows})
            files = sorted({row["file"] for row in rows})
            writer.writerow([
                name,
                fix_for(name, categories[0]),
                len(rows),
                f"{len(rows) / len(sampled):.1%}" if sampled else "0.0%",
                ",".join(categories),
                len(files),
                rows[0]["file"],
                example_text(module, rows[0]),
            ])
    return len(groups)


def main() -> None:
    parser = argparse.ArgumentParser(description="Sample mismatches evenly per file and bucket them")
    parser.add_argument("--out-dir", type=Path, default=REPO / "benchmark" / "out")
    parser.add_argument("--sample", type=int, default=1000)
    args = parser.parse_args()
    out = args.out_dir.resolve()
    module = compare_module()
    ecj_nodes, ecj_errors = module.load(out / "ecj.jsonl")
    sem_nodes, sem_errors = module.load(out / "semsitter.jsonl")
    by_file = collect(module, ecj_nodes, sem_nodes, ecj_errors, sem_errors)
    ast = sample(by_file, {"parse", "span", "ast_only_ecj", "ast_only_semsitter"}, args.sample)
    semantic = sample(by_file, {"symbol", "type", "type_arguments"}, args.sample)
    ast_path = out / "ast-issue-buckets.csv"
    semantic_path = out / "semantic-issue-buckets.csv"
    print(f"AST sampled {len(ast)} buckets {write_csv(ast_path, module, ast)}")
    print(ast_path)
    print(f"semantic sampled {len(semantic)} buckets {write_csv(semantic_path, module, semantic)}")
    print(semantic_path)


if __name__ == "__main__":
    main()
