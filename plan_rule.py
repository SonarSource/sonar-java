#!/usr/bin/env python3
"""
Plan rule implementation for S8265 ValidZoneIdCheck.
Generates technical specification and test cases based on RSPEC.
"""

import json
import os
import sys
from pathlib import Path
from anthropic import Anthropic

def load_rspec_files():
    """Load RSPEC HTML and metadata files."""
    html_path = Path("sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S8265.html")
    json_path = Path("sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S8265.json")

    with open(html_path, 'r') as f:
        html_content = f.read()

    with open(json_path, 'r') as f:
        metadata = json.load(f)

    return html_content, metadata

def generate_technical_spec(client, html_content, metadata):
    """Generate technical specification using Claude API."""

    spec_prompt = f"""Based on the following RSPEC rule specification, generate a detailed technical implementation specification for a Java sonar-java check.

Rule: {metadata['title']}
Type: {metadata['type']}
Severity: {metadata['defaultSeverity']}

RSPEC HTML:
{html_content}

Generate a technical specification that includes:

1. **Implementation Approach**
   - Which pattern to use (AbstractMethodDetection vs IssuableSubscriptionVisitor)
   - Which AST node types to visit
   - How to use MethodMatchers API

2. **Valid Zone IDs**
   - List the validation approach (IANA Time Zone Database)
   - How to get the list of valid zone IDs (use ZoneId.getAvailableZoneIds())
   - Which patterns to accept: IANA names, fixed offsets (+HH:MM), special IDs (UTC, GMT, Z)

3. **Detection Logic**
   - Match calls to ZoneId.of(String)
   - Extract string literal argument
   - Validate against valid zone IDs
   - Handle edge cases: non-literal arguments (skip), null arguments (skip)

4. **False Positive Considerations**
   - Only check string literals, not dynamic strings
   - Ensure validation is deterministic
   - Handle both uppercase and case-sensitive zone IDs correctly

5. **Quick Fix Potential**
   - Suggest valid alternatives using string similarity
   - Recommend ZoneId.systemDefault() when appropriate

6. **Implementation Pattern Examples**
   - Reference similar checks: DateFormatWeekYearCheck (validates string patterns)
   - Use AbstractMethodDetection base class
   - Use MethodMatchers for ZoneId.of() matching
   - Use asConstant() to extract string literals

Format the specification in Markdown."""

    response = client.messages.create(
        model="claude-sonnet-4-5-20250929",
        max_tokens=4000,
        messages=[{"role": "user", "content": spec_prompt}]
    )

    return response.content[0].text

def generate_test_cases(client, html_content, metadata, tech_spec):
    """Generate test cases using Claude API."""

    test_prompt = f"""Based on the following RSPEC rule specification and technical specification, generate comprehensive test cases for ValidZoneIdCheckSample.java.

Rule: {metadata['title']}
Type: {metadata['type']}

RSPEC HTML:
{html_content}

Technical Specification:
{tech_spec}

Generate a complete Java test sample file that includes:

1. **Noncompliant Cases** (should raise issues):
   - Invalid zone ID strings (e.g., "InvalidZone")
   - Three-letter abbreviations (e.g., "PST", "EST", "CST")
   - Deprecated zone IDs (e.g., "US/Pacific", "US/Eastern")
   - Completely random strings
   - Empty strings (if they cause runtime exceptions)
   - Typos in valid zone IDs (e.g., "Europe/Pari" instead of "Europe/Paris")

2. **Compliant Cases** (should not raise issues):
   - Valid IANA zone IDs (e.g., "Europe/Paris", "America/Los_Angeles", "Asia/Tokyo")
   - Fixed offset formats (e.g., "+05:30", "-08:00", "+00:00")
   - Special identifiers (e.g., "UTC", "GMT", "Z")
   - Zone offset IDs (e.g., "UTC+1", "GMT-5")
   - Dynamic strings (variables, method returns) - these should NOT be checked
   - Non-literal arguments

3. **Edge Cases**:
   - Null arguments (if applicable)
   - ZoneId.of() calls with non-string-literal arguments (should be skipped by the rule)
   - Multiple ZoneId.of() calls in same method

Format as a complete Java class named ValidZoneIdCheckSample in the default package.
Use // Noncompliant comments to mark lines that should trigger the rule.
Include clear section comments separating noncompliant and compliant cases."""

    response = client.messages.create(
        model="claude-sonnet-4-5-20250929",
        max_tokens=4000,
        messages=[{"role": "user", "content": test_prompt}]
    )

    return response.content[0].text

def main():
    # Check for Anthropic API key or use environment
    api_key = os.environ.get("ANTHROPIC_API_KEY") or os.environ.get("ANTHROPIC_AUTH_TOKEN")
    if not api_key:
        print("Error: ANTHROPIC_API_KEY or ANTHROPIC_AUTH_TOKEN environment variable not set")
        sys.exit(1)

    # Use environment variables for base_url and custom headers if available
    client_kwargs = {"api_key": api_key}
    if base_url := os.environ.get("ANTHROPIC_BASE_URL"):
        client_kwargs["base_url"] = base_url
    if custom_headers := os.environ.get("ANTHROPIC_CUSTOM_HEADERS"):
        # Parse custom headers from format "key: value"
        headers = {}
        for header in custom_headers.split(","):
            if ":" in header:
                k, v = header.split(":", 1)
                headers[k.strip()] = v.strip()
        if headers:
            client_kwargs["default_headers"] = headers

    client = Anthropic(**client_kwargs)

    print("Loading RSPEC files...")
    html_content, metadata = load_rspec_files()

    print("Generating technical specification...")
    tech_spec = generate_technical_spec(client, html_content, metadata)

    # Save technical specification
    spec_path = Path("technical_spec_S8265.md")
    with open(spec_path, 'w') as f:
        f.write(tech_spec)
    print(f"Technical specification saved to {spec_path}")

    print("\nGenerating test cases...")
    test_cases = generate_test_cases(client, html_content, metadata, tech_spec)

    # Clean up markdown code fences if present
    test_cases = test_cases.strip()
    if test_cases.startswith('```java'):
        test_cases = test_cases[7:]  # Remove ```java
    if test_cases.startswith('```'):
        test_cases = test_cases[3:]  # Remove ```
    if test_cases.endswith('```'):
        test_cases = test_cases[:-3]  # Remove closing ```
    test_cases = test_cases.strip()

    # Save test cases
    test_path = Path("java-checks-test-sources/default/src/main/java/checks/ValidZoneIdCheckSample.java")
    with open(test_path, 'w') as f:
        f.write(test_cases)
        f.write('\n')  # Ensure newline at end
    print(f"Test cases saved to {test_path}")

    print("\nPlan-rule complete!")
    print(f"\nNext steps:")
    print(f"1. Review {spec_path}")
    print(f"2. Review {test_path}")
    print(f"3. Implement ValidZoneIdCheck.java based on the specification")

if __name__ == "__main__":
    main()
