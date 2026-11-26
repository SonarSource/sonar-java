---
title: Rule metadata, registration and quality profiles
description: Generate rule descriptors from RSPEC, review profile membership and let the build register checks.
generated:
  by: tirion
---

# Rule metadata, registration and quality profiles

## Generate the descriptors from RSPEC

`sonarpedia.json` configures `rule-api` to generate Java metadata from the public [RSPEC repository](https://github.com/SonarSource/rspec). The generated HTML descriptions and JSON descriptors live in `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java`. Change the source specification in RSPEC, not the generated files. The JSON `scope` affects which source sets run the check; review it alongside the specification, not as an independent local setting.

`.github/workflows/UpdateRuleMetadata.yml` pins the `rule-api` version for the refresh workflow. For one rule, run that version of the generator from the repository root, setting `RULE_API_JAR`, `RSPEC_BRANCH` and `RULE_KEY` to the local jar, RSPEC branch and rule key first:

```bash
java -jar "$RULE_API_JAR" generate -branch "$RSPEC_BRANCH" -rule "$RULE_KEY"
```

Use the actual RSPEC branch or the default branch once the specification has landed. If RSPEC or the generator is unavailable, ask for access rather than synthesizing descriptors. Review the output and keep only changes relevant to the rule.

## Distinguish registration from activation

`check-list/src/main/java/org/sonar/java/CheckListGenerator.java` finds concrete `*Check.java` classes annotated with `@Rule` in the rule modules and reads the matching JSON metadata to place each check in main sources, test sources or both. Give the check the exact RSPEC key and follow the filename convention so it is discovered. Do not edit `GeneratedCheckList`: `check-list/pom.xml` builds it from these inputs.

A registered rule is available to the analyzer; being enabled by default is a separate choice. The built-in profiles store membership as empty files named by rule key. Review the files the generator changes against the RSPEC `defaultQualityProfiles`:

- Sonar way: `sonar-java-plugin/src/main/resources/profiles/Sonar_way`
- Sonar agentic AI: `sonar-java-plugin/src/main/resources/profiles/Sonar_agentic_AI`

Add or remove the rule's empty membership file when its profile inclusion changes; do not change unrelated membership. `sonar-java-plugin/src/main/build/ProfileJsonGenerator.java` generates the packaged profile JSON from these directories during the build. Per-rule files let separate rule changes update a profile without editing the same generated JSON array.

For externally provided checks, use the supported [registration hooks](public-api.md#extension-hooks) rather than editing Java's built-in profile inputs for another plugin.
