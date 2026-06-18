#!/usr/bin/env python3
"""
Implement rule S8265 ValidZoneIdCheck.
Generates the Java implementation based on technical specification and test cases.
"""

import json
import os
import sys
from pathlib import Path
from anthropic import Anthropic

def load_planning_files():
    """Load technical specification, test cases, and metadata."""
    tech_spec_path = Path("technical_spec_S8265.md")
    test_cases_path = Path("java-checks-test-sources/default/src/main/java/checks/ValidZoneIdCheckSample.java")
    metadata_path = Path("sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S8265.json")
    html_path = Path("sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/S8265.html")

    with open(tech_spec_path, 'r') as f:
        tech_spec = f.read()

    with open(test_cases_path, 'r') as f:
        test_cases = f.read()

    with open(metadata_path, 'r') as f:
        metadata = json.load(f)

    with open(html_path, 'r') as f:
        html_content = f.read()

    return tech_spec, test_cases, metadata, html_content

def read_existing_check():
    """Read the existing ValidZoneIdCheck stub."""
    check_path = Path("java-checks/src/main/java/org/sonar/java/checks/ValidZoneIdCheck.java")
    with open(check_path, 'r') as f:
        return f.read()

def find_similar_checks():
    """Find similar check implementations for reference."""
    checks = [
        "java-checks/src/main/java/org/sonar/java/checks/DateFormatWeekYearCheck.java",
        "java-checks/src/main/java/org/sonar/java/checks/SimpleTemporalInstantiationCheck.java",
    ]

    similar_checks = {}
    for check_path in checks:
        path = Path(check_path)
        if path.exists():
            with open(path, 'r') as f:
                similar_checks[path.name] = f.read()

    return similar_checks

def implement_check(client, tech_spec, test_cases, metadata, html_content, existing_check, similar_checks):
    """Generate the Java check implementation using Claude API."""

    implementation_prompt = f"""You are tasked with implementing a SonarJava rule check based on the technical specification and test cases provided.

Rule: {metadata['title']}
Type: {metadata['type']}
Severity: {metadata['defaultSeverity']}
Key: S8265

TECHNICAL SPECIFICATION:
{tech_spec}

TEST CASES FILE (ValidZoneIdCheckSample.java):
{test_cases}

EXISTING CHECK STUB:
{existing_check}

SIMILAR CHECK IMPLEMENTATIONS FOR REFERENCE:

DateFormatWeekYearCheck.java:
{similar_checks.get('DateFormatWeekYearCheck.java', 'Not available')}

SimpleTemporalInstantiationCheck.java:
{similar_checks.get('SimpleTemporalInstantiationCheck.java', 'Not available')}

Your task is to implement the ValidZoneIdCheck.java file based on the technical specification. The implementation should:

1. **Extend AbstractMethodDetection** as specified in the technical specification
2. **Use MethodMatchers** to detect ZoneId.of(String) calls
3. **Validate string literals** against valid zone IDs using:
   - ZoneId.getAvailableZoneIds() for IANA zone IDs
   - Pattern matching for fixed offsets (+05:30, -08:00, etc.)
   - Special identifiers (UTC, GMT, Z, UT)
   - Zone offset IDs (UTC+1, GMT-5)
4. **Only check string literals** using asConstant() to avoid false positives on dynamic strings
5. **Report issues** with helpful messages suggesting alternatives for common mistakes
6. **Follow SonarJava patterns** as seen in the similar checks provided

IMPORTANT IMPLEMENTATION DETAILS:
- Use AbstractMethodDetection as the base class (NOT IssuableSubscriptionVisitor)
- Override getMethodInvocationMatchers() to return the MethodMatchers for ZoneId.of(String)
- Override onMethodInvocationFound(MethodInvocationTree) to implement the detection logic
- Use argument.asConstant() to extract string literal values
- Cache VALID_ZONE_IDS in a static initializer for performance
- Include Levenshtein distance algorithm for suggesting corrections
- Add COMMON_CORRECTIONS map for three-letter abbreviations (PST -> America/Los_Angeles, etc.)

Generate the complete ValidZoneIdCheck.java file implementation. Output ONLY the Java code, no explanations or markdown formatting."""

    response = client.messages.create(
        model="claude-sonnet-4-5-20250929",
        max_tokens=8000,
        messages=[{"role": "user", "content": implementation_prompt}]
    )

    return response.content[0].text

def main():
    # Check for API configuration
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

    print("Loading planning files...")
    tech_spec, test_cases, metadata, html_content = load_planning_files()

    print("Reading existing check stub...")
    existing_check = read_existing_check()

    print("Finding similar check implementations...")
    similar_checks = find_similar_checks()

    print("Generating ValidZoneIdCheck implementation...")
    implementation = implement_check(client, tech_spec, test_cases, metadata, html_content, existing_check, similar_checks)

    # Clean up markdown code fences if present
    implementation = implementation.strip()
    if implementation.startswith('```java'):
        implementation = implementation[7:]  # Remove ```java
    if implementation.startswith('```'):
        implementation = implementation[3:]  # Remove ```
    if implementation.endswith('```'):
        implementation = implementation[:-3]  # Remove closing ```
    implementation = implementation.strip()

    # Save implementation
    check_path = Path("java-checks/src/main/java/org/sonar/java/checks/ValidZoneIdCheck.java")
    with open(check_path, 'w') as f:
        f.write(implementation)
        f.write('\n')  # Ensure newline at end
    print(f"Implementation saved to {check_path}")

    print("\nImplement-rule complete!")
    print(f"\nNext steps:")
    print(f"1. Review the implementation in {check_path}")
    print(f"2. Run tests: mvn test -Dtest=ValidZoneIdCheckTest")
    print(f"3. Fix any compilation or test errors")
    print(f"4. Register the check in CheckList.java")

if __name__ == "__main__":
    main()
