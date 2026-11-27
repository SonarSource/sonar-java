---
title: Keep ruling expectations reproducible
description: Check that changed SARIF expectations come from the CI ruling update workflow.
type: check
when: Pull requests add, change or remove ruling expectation reports.
actions: Report confirmed manual baselining or edits in PR Verification.
---

# Keep ruling expectations reproducible

Read `docs/systems/ruling-tests.md`. Review the diff under `its/ruling/src/test/resources/expected/` and the PR or commit evidence for CI provenance. Accept the CI bot's update, including a bot update incorporated into the source PR. Flag hand edits or local baselining when the diff or PR evidence establishes that source. Do not flag a bot-generated report just because the final commit author is human.
