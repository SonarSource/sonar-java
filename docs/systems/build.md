---
title: Setup, build and test commands
description: Build SonarJava and choose the appropriate test command and JDK.
generated:
  by: tirion
---

# Setup, build and test commands

`mise.toml` defines Maven 3.9 and Java 26, 21 and 17. Use JDK 26 for a full build, analyzer unit tests and compiling the test-source reactor. Use JDK 21 for Plugin QA and Ruling QA, as in `.github/workflows/build.yml`. `jdk.min.version` in `pom.xml` sets the plugin runtime minimum to 21, not the JDK needed to compile all test samples.

From the repository root, with [mise](https://mise.jdx.dev/) and Python 3.10+ installed:

```bash
mise install
mise tasks
mise run doctor
mise run check
```

`doctor` reports prerequisites for each task without installing tools or displaying credentials. Maven tasks require Repox access and Maven credentials. `check` runs the unit tests, dependency license check, custom-rules example and sanity test. Use `build` to install reactor artifacts without running tests. `test:unit` runs without JaCoCo unless you add `--coverage`.

```bash
mise run test:unit --module java-checks --test AbsOnNegativeCheckTest
mise run check:changed
mise run analyze:vortex
```

The changed tasks include committed, staged, unstaged and untracked changes relative to the merge base with `origin/master`. Set `MISE_CHANGE_BASE` to the intended branch or commit if that ref is missing, stale or not your review base. A changed rule with a matching test runs that test; changes that cannot be mapped to a focused test use the relevant modules instead. Focused changed checks skip the reactor-wide license-header scan. Changed checks do not replace full CI. The sanity task uses a test scanner context, not SonarQube Server.

`pom.xml` keeps the JAR manifest's `Build-Time` constant so identical inputs remain eligible for Develocity cache hits. Do not restore a time-varying value.

## Integration tests

Run `mise run build` before running an integration task on its own. The [build workflow](../../.github/workflows/build.yml) supplies credentials and `ORCHESTRATOR_CONFIG_URL`. For an authorized local run, set `ORCHESTRATOR_CONFIG_URL` to a file URL for your Orchestrator properties file. Never commit that file or its credentials. No scanner `pluginGroup` is needed in Maven settings.

```bash
mise run its:plugin --sq-version LATEST_RELEASE --test JavaTestSuite
```

The full plugin and ruling suites need a GitHub token with read access to the QA licenses. A nonblank `GITHUB_TOKEN` overrides `github.token` from the Orchestrator file for these tests. `--community-only` skips the commercial JSP test but does not remove the license requirement for `JavaTestSuite`. Use Plugin QA in CI if those prerequisites are unavailable locally.

`mise run ci` runs `check`, Plugin QA and both Linux ruling profiles. Ruling needs `git submodule update --init --recursive its/sources`. For authorized troubleshooting, use `mise run ruling without-sonarqube-project --rules S100,S101 --project guava`. Use CI for [ruling baselines and review](ruling-tests.md), not a local run.
