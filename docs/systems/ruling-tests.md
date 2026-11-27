---
title: Ruling tests and baseline updates
description: Review SonarJava findings on real projects using CI-generated SARIF changes rather than running ruling or editing baselines locally.
generated:
  by: tirion
---

# Ruling tests and baseline updates

`its/ruling/src/test/java/org/sonar/java/it/JavaRulingTest.java` analyzes real projects and compares findings with the baselines in `its/ruling/src/test/resources/expected`. Ruling catches false positives and lost true positives not covered by focused rule tests. Project sources come from the `its/sources` submodule (`ruling_java` in `.gitmodules`).

## Let CI produce baselines

Do not run ruling locally or edit the expectations by hand. The suite depends on pinned project sources, Orchestrator, Repox and the CI build of the plugin; changes in the environment can produce unrelated differences. Local verification starts with [Rule tests](rule-tests.md).

1. **Ruling QA** in `.github/workflows/build.yml` runs with JDK 21 on Linux and Windows. Windows sets `core.autocrlf=input` before checkout for consistent source lines and avoids parallel ruling because nested Maven builds share the local repository.
2. When the comparison fails, CI uploads actual reports from the ruling module's build output (the target directory's `actual` subdirectory). **Ruling Update and Notify** opens the baseline update PR when there are changes to apply. A job failure with no useful reports is not an expected baseline update; inspect the failing job first.
3. Review the update before merging it into the rule branch. Use `.github/workflows/ruling-diff-comment.yml`'s PR comment to inspect additions and removals. Accept intentional changes only. For unexpected findings or lost true positives, fix the rule and add a regression test, then let CI regenerate the update. If sources or the bot output are unavailable, ask a maintainer rather than guessing at the baselines.

## Interpret the SARIF diff

The layout beneath the expected directory is a project directory with a `java-<rule key>.sarif` report per rule that has findings. For example, see `its/ruling/src/test/resources/expected/commons-beanutils/java-S103.sarif`. Each `runs[].results[]` entry carries a `ruleId`, `message.text`, and a primary `locations[0].physicalLocation`. The latter's `artifactLocation.uri` is the project key and a project-relative file path separated by a colon, and its `region` gives the line and column range. Use the expected report's project directory to locate the corresponding checkout under `its/sources`; inspect the finding in that revision, not the latest upstream source.

For a new rule, classify added findings as true or false positives after the baseline is available and CI is green. Review all findings when practical. For a large diff, sample across projects: start with equal shares, take all findings in projects below their share, then redistribute unused slots to projects with unsampled findings. Calculate precision as true positives divided by all classified findings. If there are findings, new rules must exceed 90% precision; when they do not, add the false positives as regression cases, refine the rule, and repeat the review on fresh output. Do not infer precision from a diff with no findings to classify.
