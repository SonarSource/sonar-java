---
title: Keep rule metadata generated
description: Check that checked-in rule metadata changes come from rule-api and the canonical specification.
type: check
when: Pull requests change generated rule descriptions or metadata in the Java plugin.
actions: Report confirmed manual changes to generated rule metadata in PR Verification.
---

# Keep rule metadata generated

Read `docs/systems/rule-metadata.md`. Inspect changes to JSON or HTML under `sonar-java-plugin/src/main/resources/org/sonar/l10n/java/rules/java/` and the evidence of regeneration from RSPEC. Do not flag generated updates made by `.github/workflows/UpdateRuleMetadata.yml` or author-generated output from the documented workflow. Do not treat a missing bot author alone as evidence of a manual edit.
