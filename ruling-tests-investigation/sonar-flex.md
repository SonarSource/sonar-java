# Ruling Tests Investigation: SonarSource/sonar-flex

## Overview

The sonar-flex repository uses **LITS** (Language Integration Tests Support) with **Orchestrator** to implement ruling tests. This is a regression testing framework that validates the Flex analyzer produces consistent, expected results on a corpus of sample source code.

## Repository Structure (Ruling-related)

```
its/
  pom.xml                  # Parent POM for integration tests (modules: plugin, ruling)
  sources/                  # Git submodule -> flex-test-sources repo (commit 4b1f91f)
  plugin/                   # Plugin integration tests (separate from ruling)
  ruling/
    pom.xml                 # Ruling module POM
    src/test/
      java/org/sonar/flex/it/
        FlexRulingTest.java  # Single ruling test class
      resources/
        profile.xml          # Quality profile with 74 rules (all at INFO priority)
        expected/            # 58 JSON files, one per rule (e.g., flex-S100.json)
```

## Key Components

### 1. FlexRulingTest.java

Single test class that orchestrates the full ruling test. Here is the complete source:

```java
package org.sonar.flex.it;

import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.nio.file.Files;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FlexRulingTest {

  @ClassRule
  public static final OrchestratorRule ORCHESTRATOR = OrchestratorRule.builderEnv()
    .setEdition(Edition.ENTERPRISE_LW)
    .activateLicense()
    .useDefaultAdminCredentialsForBuilds(true)
    .setSonarVersion(Configuration.createEnv().getString("sonar.runtimeVersion"))
    .addPlugin(FileLocation.byWildcardMavenFilename(
        new File("../../sonar-flex-plugin/target"), "sonar-flex-plugin-*.jar"))
    .addPlugin(MavenLocation.of(
        "org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.11.0.2659"))
    .restoreProfileAtStartup(FileLocation.of("src/test/resources/profile.xml"))
    .build();

  @Test
  public void test() throws Exception {
    ORCHESTRATOR.getServer().provisionProject("project", "project");
    ORCHESTRATOR.getServer().associateProjectToQualityProfile("project", "flex", "rules");
    File litsDifferencesFile = FileLocation.of("target/differences").getFile();

    SonarScanner build = SonarScanner.create(FileLocation.of("../sources/src").getFile())
      .setProperty("sonar.scanner.skipJreProvisioning", "true")
      .setProjectKey("project")
      .setProjectName("project")
      .setProjectVersion("1")
      .setSourceDirs(".")
      .setSourceEncoding("UTF-8")
      .setProperty("sonar.lits.dump.old",
          FileLocation.of("src/test/resources/expected").getFile().getAbsolutePath())
      .setProperty("sonar.lits.dump.new",
          FileLocation.of("target/actual").getFile().getAbsolutePath())
      .setProperty("sonar.lits.differences",
          litsDifferencesFile.getAbsolutePath())
      .setProperty("sonar.cpd.exclusions", "**/*")
      .setDebugLogs(true)
      .setEnvironmentVariable("SONAR_RUNNER_OPTS", "-Xmx1000m");
    ORCHESTRATOR.executeBuild(build);

    assertThat(Files.readAllBytes(litsDifferencesFile.toPath())).isEmpty();
  }
}
```

### 2. How the Test Works (Step by Step)

1. **Orchestrator starts a SonarQube server** (Enterprise Lightweight edition) with:
   - The sonar-flex-plugin (built from local `target/` directory)
   - The `sonar-lits-plugin` v0.11.0.2659 (fetched from Maven)
   - A quality profile (`profile.xml`) with 74 Flex rules pre-loaded

2. **A project is provisioned** on the SonarQube server and associated with the "rules" quality profile.

3. **SonarScanner analyzes** the source code from `its/sources/src` (a git submodule pointing to external test sources).

4. **LITS plugin intercepts** all issues raised during analysis and dumps them to `target/actual/` as JSON files (one per rule).

5. **LITS compares** actual results against the expected baseline in `src/test/resources/expected/` (58 JSON files).

6. **Differences are written** to `target/differences`. The test asserts this file is empty (no regressions).

### 3. LITS (sonar-lits-plugin)

- Repository: [SonarSource/sonar-lits](https://github.com/SonarSource/sonar-lits)
- Version used: `0.11.0.2659`
- Fetched via Maven coordinates: `org.sonarsource.sonar-lits-plugin:sonar-lits-plugin`
- Key properties:
  - `sonar.lits.dump.old` - path to expected results (baseline)
  - `sonar.lits.dump.new` - path where actual results are written
  - `sonar.lits.differences` - path to the differences output file
- Each JSON file maps files to line numbers where issues are raised for a specific rule.

### 4. Expected Results

The `expected/` directory contains 58 JSON files, one per rule. Examples:
- `flex-ClassComplexity.json`
- `flex-S100.json` through `flex-S4507.json`
- `flex-LineLength.json`
- `flex-CommentedCode.json`

These files are the regression baseline. When a rule is added or modified, the developer must update the corresponding expected file after reviewing the differences.

### 5. Quality Profile (profile.xml)

- Profile name: `rules`
- Language: `flex`
- Contains 74 rules from the `flex` repository
- All rules set to INFO priority
- Some rules have parameters (e.g., `FunctionComplexity` threshold=10, `LineLength` max=80)

### 6. Test Sources

- Stored as a git submodule at `its/sources` pointing to a separate `flex-test-sources` repository
- The scanner analyzes `its/sources/src` with `sourceDir=.`

## CI Integration

### GitHub Actions Workflow (`build.yml`)

The ruling test runs as a separate job in the CI pipeline:

```yaml
ruling:
  name: Ruling
  runs-on: github-ubuntu-latest-s
  needs: [build]
  if: >
    github.event_name == 'pull_request' ||
    github.ref == 'refs/heads/master' ||
    startsWith(github.ref, 'refs/heads/branch-') ||
    startsWith(github.ref, 'refs/heads/dogfood-')
  steps:
    - Checkout (with submodules: true)
    - Setup mise (toolchain)
    - Download built JAR
    - Load secrets from Vault
    - Configure Maven
    - Run Ruling:
        cd its/ruling
        mvn verify -Pit-ruling -Dsonar.runtimeVersion=LATEST_RELEASE \
          -Dmaven.test.redirectTestOutputToFile=false -B -e -V
```

Key points:
- Runs on PRs, master, branch-*, and dogfood-* branches
- Depends on the `build` job (needs the plugin JAR)
- Uses `-Pit-ruling` Maven profile which sets `skipTests=false`
- Tests against `LATEST_RELEASE` of SonarQube
- Requires QA environment secrets

### Maven Profile

The `it-ruling` profile in `its/ruling/pom.xml` simply sets `skipTests=false`. Tests are skipped by default (set in parent `its/pom.xml`).

## Dependencies

- `sonar-orchestrator-junit4` - JUnit4 integration for Orchestrator
- `junit` - JUnit 4
- `assertj-core` - Fluent assertions
- All are test-scoped dependencies

## Key Differences from sonar-java

1. **Much simpler**: Single test class, single test method, single project to analyze
2. **No autoscan step**: Only one ruling test run (no separate "without semantic" analysis)
3. **JUnit 4**: Uses `OrchestratorRule` with `@ClassRule` (JUnit 4 pattern), not JUnit 5
4. **Single quality profile**: One profile with all 74 rules (sonar-java has separate profiles)
5. **Smaller scale**: 58 expected result files vs. hundreds in sonar-java
6. **LITS plugin**: Same LITS-based approach as sonar-java for comparing expected vs actual results
7. **Git submodule for sources**: Uses a submodule for test sources (same pattern as sonar-java)

## Summary

The sonar-flex ruling tests follow the standard SonarSource LITS pattern:
- **Orchestrator** provisions a SonarQube server with the analyzer plugin and LITS plugin
- **SonarScanner** analyzes a corpus of Flex source code
- **LITS** captures all issues raised and compares them against a stored expected baseline
- The test **passes if and only if** the differences file is empty (no unexpected changes)

This is the simplest form of the LITS ruling test pattern used across SonarSource analyzers, with a single test class and a single analysis run.
