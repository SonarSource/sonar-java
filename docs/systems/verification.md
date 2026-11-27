---
title: Verification before merge
description: Select local checks and review SonarJava CI evidence before merging.
sources:
  - resource: "5742002202"
generated:
  by: tirion
---

# Verification before merge

Run the relevant checks in [build and test commands](build.md). Run the focused verifier test for rule behavior, compile changed Maven samples, and use [license checks](licenses.md) for dependencies. Check docs with the commands in `.github/workflows/docs.yml`. Record what passed and what could not run; a local unit test is not a substitute for an applicable skipped integration check.

`.github/workflows/build.yml` is the source of truth for job execution and prerequisites:

| Job | Evidence to review |
| --- | --- |
| Build | Packages a candidate without running tests and excludes the test-sample leaf modules selected by `--projects`. |
| Ruling QA | Checks SARIF against expected results on Linux and Windows. |
| Ruling Update and Notify | Handles ruling differences on PRs; review the bot output, not just the job status. |
| Plugin QA | Exercises analyzer integration against SonarQube Server. |
| Sanity Test | Compiles test sources and runs all rules to catch analyzer crashes. |
| Test Analyze | Builds and analyzes the repository with SonarQube. |
| Custom Rules and License Check | Builds the example using `docs/java-custom-rules-example/pom_SQ_10_6_LATEST.xml` as the compatibility floor exercised by CI and checks third-party licenses. |
| Build and Unit Test on Windows | Runs Maven `clean verify` independently of the candidate build. |
| Promote | Depends on the candidate build and its verification jobs. |

`.github/workflows/ruling-diff-comment.yml` posts the SARIF comparison when expectations change; inspect actual issue locations and false positives before accepting a new baseline. `.github/workflows/docs.yml` checks the documentation bundle and agent entry points on every PR.

## Review and merge

Use the actual required checks and review rules shown on the PR; `.github/CODEOWNERS` identifies the owning team. Public contributions and high-risk changes need human review. Treat public API, generated metadata, baselines, licensing, and changes in parser, cache or analysis behavior as requiring explicit review of their impact, not routine low-risk cleanup. Code review automation does not replace branch protections or maintainer approval.

## Performance validation

`.github/workflows/build.yml` publishes a deployed candidate version for performance validation. `.github/workflows/pvf-comment.yml` responds to an authorized maintainer's **/pvf** PR comment and dispatches validation in the performance repository. Use it for parser, semantic analysis, caching or other hot-path changes when CI unit and ruling results alone cannot show performance impact. Inspect the comparison for the exact candidate rather than reusing results from a previous build.
