---
title: Test new rules with and without semantic resolution
description: Check that each new rule has semantic and no-semantic tests with intentional expectations.
type: check
when: Pull requests introduce a new Java analyzer rule.
actions: Report missing verifier coverage in PR Verification.
---

# Test new rules with and without semantic resolution

Read `docs/systems/rule-tests.md`. For each newly introduced rule, look for a `CheckVerifier` test using semantic resolution and a test using `withoutSemantic()`. An assertion that the rule reports no issues without semantic resolution is valid when that is the intended behavior. Accept equivalent parameterized or shared coverage when both modes are actually exercised; do not require a new class purely for naming.
