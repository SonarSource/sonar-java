#!/usr/bin/env python3
"""
Create GitHub pull request for S8265 ValidZoneIdCheck.
Adapted from sonar-skunk's rule-factory create_pr task.
"""

import json
import subprocess
import sys
from pathlib import Path

def get_rule_metadata():
    """Load rule metadata."""
    metadata_path = Path("sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S8265.json")
    with open(metadata_path, 'r') as f:
        return json.load(f)

def get_branch_name():
    """Get current branch name."""
    result = subprocess.run(
        ["git", "branch", "--show-current"],
        capture_output=True,
        text=True,
        check=True
    )
    return result.stdout.strip()

def push_branch(branch_name):
    """Push branch to origin."""
    print(f"Pushing branch {branch_name} to origin...")
    subprocess.run(
        ["git", "push", "-u", "origin", branch_name],
        check=True
    )
    print(f"Pushed branch {branch_name}")

def check_existing_pr(branch_name):
    """Check if PR already exists for this branch."""
    result = subprocess.run(
        ["gh", "pr", "list", "--head", branch_name, "--json", "number,url,title"],
        capture_output=True,
        text=True,
        check=True
    )
    prs = json.loads(result.stdout)
    return prs[0] if prs else None

def generate_pr_body(metadata):
    """Generate PR body content."""
    title = metadata['title']
    rule_type = metadata['type']
    severity = metadata['defaultSeverity']
    tags = ', '.join(metadata.get('tags', []))

    return f"""## Rule: S8265 - {title}

**Type:** {rule_type}
**Severity:** {severity}
**Tags:** {tags}

### Description

This rule detects invalid time zone identifier strings passed to `ZoneId.of()` method.

### Implementation Details

- Extends `AbstractMethodDetection` to detect `ZoneId.of(String)` calls
- Validates string literals against:
  - IANA Time Zone Database identifiers (using `ZoneId.getAvailableZoneIds()`)
  - Fixed offset formats (+05:30, -08:00, etc.)
  - Special identifiers (UTC, GMT, Z, UT)
  - Zone offset IDs (UTC+1, GMT-5)
- Provides helpful suggestions for common mistakes using Levenshtein distance
- Maps three-letter abbreviations (PST, EST, etc.) to correct zone IDs

### Test Coverage

All tests pass:
- Invalid zone IDs detection
- Three-letter abbreviations with suggestions
- Typo detection with suggestions
- Case sensitivity validation
- Dynamic string handling (no false positives)

### Related Issues

- Jira: RIS-492
- RSPEC PR: https://github.com/SonarSource/rspec/pull/5849

---

🤖 Generated with Claude Code (claude.com/code)
"""

def create_pull_request(branch_name, metadata, jira_key="RIS-492"):
    """Create draft pull request using gh CLI."""
    title = metadata['title']
    pr_title = f"{jira_key} Create rule S8265: {title}"
    pr_body = generate_pr_body(metadata)

    print(f"Creating draft PR for {branch_name}...")
    print(f"Title: {pr_title}")

    result = subprocess.run(
        ["gh", "pr", "create",
         "--draft",
         "--title", pr_title,
         "--body", pr_body,
         "--head", branch_name,
         "--base", "master"],
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        # Check if error is due to existing PR
        if "already exists" in result.stderr.lower():
            print("PR already exists for this branch")
            existing_pr = check_existing_pr(branch_name)
            if existing_pr:
                print(f"Existing PR: {existing_pr['url']}")
                return existing_pr['url']
        else:
            print(f"Error creating PR: {result.stderr}")
            sys.exit(1)

    pr_url = result.stdout.strip()
    print(f"Created draft PR: {pr_url}")
    return pr_url

def main():
    print("Loading rule metadata...")
    metadata = get_rule_metadata()

    print("Getting current branch...")
    branch_name = get_branch_name()
    print(f"Branch: {branch_name}")

    # Check if PR already exists
    existing_pr = check_existing_pr(branch_name)
    if existing_pr:
        print(f"PR already exists for branch {branch_name}")
        print(f"URL: {existing_pr['url']}")
        print(f"Title: {existing_pr['title']}")
        return

    # Push branch to origin
    push_branch(branch_name)

    # Create PR
    pr_url = create_pull_request(branch_name, metadata)

    print("\nCreate-PR complete!")
    print(f"\nPull Request: {pr_url}")
    print(f"\nNext steps:")
    print(f"1. Review the PR description")
    print(f"2. Ensure all CI checks pass")
    print(f"3. Mark as ready for review when complete")

if __name__ == "__main__":
    main()
