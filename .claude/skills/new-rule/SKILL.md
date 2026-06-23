---
name: new-rule
description: Guidelines for implementing new rules in sonar-java
---

# New Rule Implementation Skill

This skill provides sonar-java-specific guidelines for implementing new rules.

## What to Do

### 1. Metadata Files
- **DO** add the rule's HTML and JSON files to:
  - `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/{RULE_ID}.html`
  - `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/{RULE_ID}.json`
- **DO** add the rule to the SonarWay and Sonar Agentic AI profile:
  - `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/Sonar_way_profile.json`
  - `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/Sonar_agentic_AI_profile.json`

### 2. Tests
- Update ruling tests located in `its/ruling/` after implementing the rule. These tests verify the rule against real-world Java projects
- Run JavaAgenticWayProfileTest
- Update AutoScanTest assertions if needed

### 3. External Dependencies in Tests
- **DO NOT** add external library dependencies for test samples
- **DO** create mock-ups or use non-compiling tests when external libraries are needed
- **SEE** Spring examples in `java-checks-test-sources/*/src/main/files/non-compiling/checks/`

## What NOT to Do

### 1. Version Control
- **DO NOT** commit log files (e.g., `*.log`, build logs)
- **DO NOT** commit temporary or generated files

### 2. Build Configuration
- **DO NOT** modify `pom.xml` files
- Build configuration is managed centrally
- Only modify if explicitly required by the rule implementation

### 3. Architecture
- **DO NOT** make architectural changes to the codebase
- New rules should fit within the existing architecture
- Follow the patterns used by similar existing rules

### 4. Test Dependencies
- **DO NOT** add real external dependencies to test projects
- Use mocks, stubs, or non-compiling tests instead
- Keep test projects lightweight and self-contained
- **DO NOT** modify the setup part of AutoScanTest (only the assert part if needed)

## File Structure Reference

### Rule Implementation
- Rule class: `java-checks/src/main/java/org/sonar/java/checks/{RuleId}Check.java`
- Test class: `java-checks/src/test/java/org/sonar/java/checks/{RuleId}CheckTest.java`
- Test samples: `java-checks-test-sources/default/src/main/files/checks/{RuleId}CheckSample.java`

### Metadata
- HTML description: `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/{RULE_ID}.html`
- JSON metadata: `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/{RULE_ID}.json`
- Profile: `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/Sonar_way_profile.json`

### Non-Compiling Tests (when needed)
- Location: `java-checks-test-sources/default/src/main/files/non-compiling/checks/`
- Use for: Code that requires external libraries (Spring, AWS SDK, etc.)
