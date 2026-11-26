---
title: Release checks
description: Verify a SonarJava release candidate before handing it to release automation.
generated:
  by: tirion
---

# Release checks

1. Check the intended branch, commit and version. Verify **Build** and the applicable [verification jobs](verification.md), including compatibility, licenses and rule metadata. Resolve ruling deltas and investigate any applicable performance regression before using a candidate.
2. Confirm promotion and the releasability result for that candidate. `.github/workflows/ReleasabilityCheck.yml` evaluates successful builds on tracked release and development branches; `.github/workflows/releasability.yaml` supports an explicit artifact version. Do not assume a status for the latest build applies to a different candidate.
3. Confirm intended SonarQube Server, Cloud and IDE integrations with the release owner. `.github/workflows/automated-release.yml` is the release entry point and owns integration and version-bump options. `.github/workflows/release.yml` is the publishing step, not a local test. Its dry-run/publish behavior and that of the automated workflow are defined in those workflows; do not dispatch either without authorization.
4. For releases of the ECJ fork or symbolic execution engine, use the separate references in [external context](external-context.md#java-analyzer-and-related-projects).
