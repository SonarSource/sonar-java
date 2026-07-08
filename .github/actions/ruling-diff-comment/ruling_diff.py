#!/usr/bin/env python3
"""Generate and post ruling diff comment on PRs."""

from __future__ import annotations

import argparse
import json
import logging
import os
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

EXPECTED_RULING_ROOT = "its/ruling/src/test/resources"
COMMENT_MARKER = "<!-- ruling-diff-comment -->"
COMMENT_SOFT_LIMIT = 60000  # GitHub comment size limit


def configure_logging() -> None:
    """Configure logging based on environment."""
    level = logging.DEBUG if os.environ.get("RUNNER_DEBUG") else logging.INFO
    logging.basicConfig(level=level, format="%(asctime)s %(levelname)s %(message)s")


def parse_args() -> argparse.Namespace:
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description="Generate and post ruling diff comment"
    )
    parser.add_argument("--pr-number", required=True)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--base-sha", required=True)
    parser.add_argument("--head-sha", required=True)
    args = parser.parse_args()
    if "/" not in args.repository:
        raise ValueError("--repository must be in owner/repo format")
    return args


def run_command(command: list[str]) -> str:
    """Run a command and return stdout."""
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(
            f"Command failed: {' '.join(command)}\n"
            f"stdout: {result.stdout}\nstderr: {result.stderr}"
        )
    return result.stdout


def get_changed_ruling_files(base_sha: str, head_sha: str) -> list[str]:
    """Get list of changed ruling JSON files."""
    output = run_command(
        [
            "git",
            "diff",
            "--name-only",
            f"{base_sha}...{head_sha}",
            "--",
            f"{EXPECTED_RULING_ROOT}/",
        ]
    )
    changed = [
        path.strip()
        for path in output.splitlines()
        if path.strip().endswith(".json")
        and path.strip().startswith(f"{EXPECTED_RULING_ROOT}/")
    ]
    return sorted(set(changed))


def load_json_at_ref(path: str, ref: str) -> dict[str, list[int]] | None:
    """Load a JSON file at a specific git ref."""
    result = subprocess.run(
        ["git", "show", f"{ref}:{path}"], capture_output=True, text=True, check=False
    )
    if result.returncode != 0:
        # File doesn't exist at this ref
        if "does not exist" in result.stderr or "exists on disk, but not in" in result.stderr:
            return None
        raise RuntimeError(
            f"Failed to read file at ref: git show {ref}:{path}\n{result.stderr}"
        )
    try:
        data = json.loads(result.stdout)
        if not isinstance(data, dict):
            return {}
        return data
    except json.JSONDecodeError:
        return {}


def get_submodule_commit_at_ref(submodule_path: str, ref: str) -> str | None:
    """Get the commit SHA that a submodule points to at a specific main repo ref."""
    result = subprocess.run(
        ["git", "ls-tree", ref, submodule_path],
        capture_output=True,
        text=True,
        check=False
    )
    if result.returncode != 0:
        return None

    # Output format: "160000 commit <sha>\t<path>"
    parts = result.stdout.strip().split()
    if len(parts) >= 3 and parts[1] == "commit":
        return parts[2]
    return None


def load_source_file_at_ref(file_path: str, ref: str) -> str | None:
    """Load source file content at a specific git ref.

    For files in submodules, this resolves the submodule commit first.
    """
    # Check if the file is in a submodule (its/sources/)
    if file_path.startswith("its/sources/"):
        # Get the submodule commit at this ref
        submodule_commit = get_submodule_commit_at_ref("its/sources", ref)
        if not submodule_commit:
            return None

        # Extract the path within the submodule
        # file_path is like "its/sources/guava/src/..."
        # We need just "guava/src/..."
        submodule_relative_path = file_path[len("its/sources/"):]

        # Read from the submodule's repository using the submodule commit
        result = subprocess.run(
            ["git", "-C", "its/sources", "show", f"{submodule_commit}:{submodule_relative_path}"],
            capture_output=True,
            text=True,
            check=False
        )
        if result.returncode != 0:
            return None
        return result.stdout

    # For non-submodule files, use the standard approach
    result = subprocess.run(
        ["git", "show", f"{ref}:{file_path}"], capture_output=True, text=True, check=False
    )
    if result.returncode != 0:
        return None
    return result.stdout


def get_code_snippet(source_path: str, line_number: int, ref: str, context_lines: int = 5) -> str | None:
    """Get a code snippet around a specific line."""
    content = load_source_file_at_ref(source_path, ref)
    if content is None:
        return None

    lines = content.splitlines()
    if line_number < 1 or line_number > len(lines):
        return None

    # Calculate line range (1-indexed to 0-indexed)
    start = max(0, line_number - context_lines - 1)
    end = min(len(lines), line_number + context_lines)

    snippet_lines = []
    for i in range(start, end):
        line_num = i + 1
        prefix = ">>> " if line_num == line_number else "    "
        # Pad line numbers to align properly
        snippet_lines.append(f"{prefix}{line_num:5d} | {lines[i]}")

    return "\n".join(snippet_lines)


def parse_rule_filename(filename: str) -> str | None:
    """Parse rule key from filename (e.g., java-S1118.json -> S1118)."""
    match = re.search(r"java-([^/]+)\.json$", filename)
    return match.group(1) if match else None


def diff_issues(
    base_issues: dict[str, list[int]] | None, head_issues: dict[str, list[int]] | None
) -> tuple[dict[str, list[int]], dict[str, list[int]]]:
    """Calculate added and removed issues."""
    base_issues = base_issues or {}
    head_issues = head_issues or {}

    added: dict[str, list[int]] = {}
    removed: dict[str, list[int]] = {}

    all_files = set(base_issues.keys()) | set(head_issues.keys())

    for file_path in all_files:
        base_lines = set(base_issues.get(file_path, []))
        head_lines = set(head_issues.get(file_path, []))

        added_lines = sorted(head_lines - base_lines)
        removed_lines = sorted(base_lines - head_lines)

        if added_lines:
            added[file_path] = added_lines
        if removed_lines:
            removed[file_path] = removed_lines

    return added, removed


def resolve_source_path(project: str, file_path: str) -> str:
    """Resolve the actual source file path from the ruling JSON key."""
    # Ruling JSON keys are like: "com.google.guava:guava:src/..."
    # We need to extract the actual file path and map to its/sources/{project}/

    # Parse the file path from the ruling JSON key
    # Format is typically: "maven.groupId:artifactId:relative/path/to/file"
    parts = file_path.split(":", 2)
    if len(parts) >= 3:
        # Has maven coordinates: groupId:artifactId:path
        actual_path = parts[2]
    elif len(parts) == 2:
        # Format: "project:path"
        actual_path = parts[1]
    else:
        # Format: just "path"
        actual_path = file_path

    # Sources are in its/sources/{project}/ relative to repo root
    source_path = f"its/sources/{project}/{actual_path}"
    return source_path


def format_issue_with_snippet(
    file_path: str, line_numbers: list[int], ref: str, project: str, issue_type: str
) -> list[str]:
    """Format issues with code snippets."""
    lines = []
    short_path = file_path.split(":", 2)[-1] if ":" in file_path else file_path

    # Limit to first 3 issues per file to avoid huge comments
    for line_num in sorted(line_numbers[:3]):
        lines.append("")
        lines.append(f"**{issue_type}** `{short_path}` (line {line_num})")

        # Try to get source path and snippet
        source_path = resolve_source_path(project, file_path)
        snippet = get_code_snippet(source_path, line_num, ref)

        if snippet:
            lines.append("```java")
            lines.append(snippet)
            lines.append("```")
        else:
            # Source file not found - might be binary, generated, or path resolution issue
            lines.append(f"*(Code snippet not available)*")

    if len(line_numbers) > 3:
        lines.append("")
        lines.append(f"*... and {len(line_numbers) - 3} more issue(s) in this file*")

    return lines


def format_comment(rule_diffs: dict[str, dict[str, Any]], head_sha: str, base_sha: str) -> str:
    """Format the PR comment with ruling diff information."""
    lines = [
        COMMENT_MARKER,
        "## Ruling Diff Summary",
        "",
    ]

    total_added = sum(
        sum(len(issues) for issues in diff["added"].values())
        for diff in rule_diffs.values()
    )
    total_removed = sum(
        sum(len(issues) for issues in diff["removed"].values())
        for diff in rule_diffs.values()
    )

    lines.append(
        f"Detected changes in {len(rule_diffs)} rule file(s): "
        f"{total_removed} issue(s) removed, {total_added} issue(s) added."
    )

    for rule_key, diff in sorted(rule_diffs.items()):
        project = diff["project"]
        added = diff["added"]
        removed = diff["removed"]

        added_count = sum(len(issues) for issues in added.values())
        removed_count = sum(len(issues) for issues in removed.values())

        # Determine if it's a new ruling file
        is_new = removed_count == 0 and added_count > 0
        status = " - new ruling file" if is_new else ""

        lines.append("<details>")
        summary = (
            f"<summary><b>{rule_key}</b> (<code>java</code>) on <b>{project}</b> - "
            f"{removed_count} issues removed, {added_count} issues added{status}</summary>"
        )
        lines.append(summary)
        lines.append("")

        # Show removed issues with snippets
        if removed:
            for file_path, line_numbers in sorted(removed.items())[:5]:  # Limit to 5 files
                lines.extend(
                    format_issue_with_snippet(
                        file_path, line_numbers, base_sha, project, "Removed"
                    )
                )
            if len(removed) > 5:
                lines.append("")
                lines.append(f"*... and {len(removed) - 5} more file(s) with removed issues*")

        # Show added issues with snippets
        if added:
            for file_path, line_numbers in sorted(added.items())[:5]:  # Limit to 5 files
                lines.extend(
                    format_issue_with_snippet(
                        file_path, line_numbers, head_sha, project, "Added"
                    )
                )
            if len(added) > 5:
                lines.append("")
                lines.append(f"*... and {len(added) - 5} more file(s) with added issues*")

        lines.append("")
        lines.append("</details>")
        lines.append("")

    comment = "\n".join(lines)

    # Truncate if too long
    if len(comment) > COMMENT_SOFT_LIMIT:
        # Find a good truncation point
        truncated_lines = []
        current_length = 0
        for line in lines:
            if current_length + len(line) > COMMENT_SOFT_LIMIT - 500:
                break
            truncated_lines.append(line)
            current_length += len(line) + 1

        truncated_lines.append("")
        truncated_lines.append("</details>")
        truncated_lines.append("")
        truncated_lines.append("⚠️ **Comment truncated due to size.** View full details in the workflow run.")
        comment = "\n".join(truncated_lines)

    return comment


def get_existing_comment_id(pr_number: str, repository: str) -> str | None:
    """Find existing ruling diff comment on the PR."""
    try:
        output = run_command(
            ["gh", "api", f"repos/{repository}/issues/{pr_number}/comments", "--paginate"]
        )
        # Parse multiple JSON documents from paginated response
        comments = []
        for line in output.strip().split("\n"):
            if line.strip().startswith("["):
                comments.extend(json.loads(line))

        for comment in comments:
            if COMMENT_MARKER in comment.get("body", ""):
                return str(comment["id"])
    except Exception as e:
        logging.warning(f"Failed to get existing comments: {e}")
    return None


def post_or_update_comment(pr_number: str, repository: str, body: str) -> None:
    """Post a new comment or update existing one."""
    comment_id = get_existing_comment_id(pr_number, repository)

    if comment_id is None:
        logging.info(f"Posting new ruling diff comment on PR #{pr_number}")
        run_command(
            ["gh", "pr", "comment", pr_number, "--repo", repository, "--body", body]
        )
    else:
        logging.info(f"Updating existing ruling diff comment {comment_id} on PR #{pr_number}")
        run_command(
            [
                "gh",
                "api",
                "--method",
                "PATCH",
                f"repos/{repository}/issues/comments/{comment_id}",
                "-f",
                f"body={body}",
            ]
        )


def main() -> None:
    """Main entry point."""
    configure_logging()
    args = parse_args()

    if not (args.pr_number.strip() and args.base_sha.strip() and args.head_sha.strip()):
        logging.info("Missing pr/base/head arguments. Skipping ruling diff comment.")
        return

    changed_files = get_changed_ruling_files(args.base_sha, args.head_sha)
    if not changed_files:
        logging.info("No changed ruling json files found. Nothing to do.")
        return

    logging.info(f"Found {len(changed_files)} changed ruling json files")

    rule_diffs: dict[str, dict[str, Any]] = {}

    for file_path in changed_files:
        rule_key = parse_rule_filename(file_path)
        if not rule_key:
            logging.warning(f"Could not parse rule key from {file_path}")
            continue

        # Extract project name from path
        # its/ruling/src/test/resources/{project}/java-{rule}.json
        parts = Path(file_path).parts
        if len(parts) >= 6:
            project = parts[5]
        else:
            project = "unknown"

        base_issues = load_json_at_ref(file_path, args.base_sha)
        head_issues = load_json_at_ref(file_path, args.head_sha)

        added, removed = diff_issues(base_issues, head_issues)

        if added or removed:
            rule_diffs[rule_key] = {
                "project": project,
                "added": added,
                "removed": removed,
            }

    if not rule_diffs:
        logging.info("Changed files have no issue deltas. No comment will be posted.")
        return

    comment = format_comment(rule_diffs, args.head_sha, args.base_sha)
    post_or_update_comment(args.pr_number, args.repository, comment)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        logging.error(f"Failed to generate ruling diff comment: {exc}", exc_info=True)
        sys.exit(1)
