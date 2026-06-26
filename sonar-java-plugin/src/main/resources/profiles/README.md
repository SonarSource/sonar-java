# Quality Profile Definitions

This directory contains quality profile definitions for the SonarQube Java analyzer.

## Structure

Each subdirectory represents a quality profile:

- `sonar_way/` - Rules in the "Sonar way" profile
- `sonar_agentic_ai/` - Rules in the "Sonar agentic AI" profile

## How to Add a Rule to a Profile

To add a rule to a quality profile, simply create an empty file named after the rule key in the appropriate profile directory.

**Example:** To add rule S8910 to both profiles:

```bash
touch sonar-java-plugin/src/main/resources/profiles/sonar_way/S8910
touch sonar-java-plugin/src/main/resources/profiles/sonar_agentic_ai/S8910
```

## How to Remove a Rule from a Profile

Delete the corresponding file from the profile directory:

```bash
rm sonar-java-plugin/src/main/resources/profiles/sonar_way/S1234
```

## Build Process

During the Maven build, the `ProfileJsonGenerator` reads these directories and generates the profile JSON files at:
- `target/generated-resources/profiles/org/sonar/l10n/java/rules/java/Sonar_way_profile.json`
- `target/generated-resources/profiles/org/sonar/l10n/java/rules/java/Sonar_agentic_AI_profile.json`

These generated files are then packaged into the plugin JAR.

## Benefits of This Approach

1. **No merge conflicts** - Parallel PRs adding different rules create different files
2. **Clear review** - `git diff` shows exactly which rules were added/removed
3. **Simple** - No need to edit large JSON arrays or maintain complex metadata
4. **Maintainable** - One file per rule makes it easy to track profile membership
