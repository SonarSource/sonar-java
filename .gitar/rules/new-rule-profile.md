---
title: Check profile membership for new rules
description: Check that each new rule has an intentional profile decision and matching marker when enabled.
type: check
when: Pull requests introduce a new Java analyzer rule.
actions: Report missing profile markers or missing opt-in decisions in PR Verification.
---

# Check profile membership for new rules

Read `docs/systems/rule-metadata.md`. Match the new rule key against the appropriate empty marker file in `sonar-java-plugin/src/main/resources/profiles/Sonar_way/` or `sonar-java-plugin/src/main/resources/profiles/Sonar_agentic_AI/`. If it belongs in a built-in profile, flag missing or wrong markers. If an explicitly documented profile decision keeps the rule opt-in, do not suggest enabling it just to satisfy the check.
