# Ruling Tests in SonarSource/sonar-html

## Overview

The sonar-html repository uses a LITS-based ruling test approach with a single test class,
Orchestrator-managed SonarQube instances, and CI automation that auto-updates expected results
on failure.

## Repository Structure

```
its/
  pom.xml                          # Parent POM for all integration tests
  sources/                         # Git submodule -> SonarCommunity/web-test-sources
  plugin/                          # Plugin integration tests
  sonarlint-tests/                 # SonarLint integration tests
  ruling/
    pom.xml                        # Ruling module POM
    src/test/
      java/org/sonar/web/it/
        WebRulingTest.java         # Single ruling test class
      resources/expected/
        Web-*.json                 # 75 expected result files (one per rule)
```

## Key Components

### 1. Test Sources (Git Submodule)

- Path: `its/sources`
- Repository: `https://github.com/SonarCommunity/web-test-sources.git`
- Contains real-world HTML/JSP/PHP/ERB/XHTML files from projects like Silverpeas, WebKit, etc.

### 2. LITS Plugin

The test uses `sonar-lits-plugin` (version `0.11.0.2659`) loaded from Maven Central:
```java
.addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.11.0.2659"))
```

LITS (Lightweight Integration Test Suite) compares actual analysis results against expected
baselines stored as JSON files.

### 3. Orchestrator

Uses `OrchestratorRule` (JUnit 4 `@ClassRule`) to spin up a SonarQube instance:
- Edition: `ENTERPRISE_LW` (Enterprise Lightweight)
- SonarQube version: controlled by `sonar.runtimeVersion` system property, defaults to `LATEST_RELEASE`
- The HTML plugin JAR is loaded from `../../sonar-html-plugin/target/sonar-html-plugin-*.jar`

### 4. Test Class: WebRulingTest.java

**Location:** `its/ruling/src/test/java/org/sonar/web/it/WebRulingTest.java`

**Setup (`@BeforeClass`):**
1. Uses `ProfileGenerator` (from `sonar-analyzer-commons`) to generate a quality profile
   that activates all rules from the `Web` repository for the `web` language.
2. Instantiates 6 template-based rules:
   - `IllegalAttributeCheck` -> `Template_DoNotUseNameProperty`
   - `S8687` -> `Template_AllowedLang`
   - `S8488` -> `Template_RequiredScriptType`
   - `S8488` -> `Template_RequiredInputType`
   - `S8551` -> `Template_ForbiddenBlinkTag`
   - `S140` -> `Template_XPathBlinkTag`

**Test method (`ruling()`):**
1. Provisions a project and associates it with the generated quality profile.
2. Runs `SonarScanner` on the test sources with these key properties:
   - `sonar.html.file.suffixes`: `xhtml,html,php,erb`
   - `sonar.jsp.file.suffixes`: `jspf,jsp`
   - `sonar.lits.dump.old`: points to `src/test/resources/expected`
   - `sonar.lits.dump.new`: points to `target/actual`
   - `sonar.lits.differences`: points to `target/differences`
   - `sonar.cpd.exclusions`: `**/*` (disable copy-paste detection)
   - Memory: 1024MB via `SONAR_RUNNER_OPTS`
3. Asserts no ERROR lines in the build log.
4. Asserts the differences file is empty (meaning actual matches expected).

### 5. Expected Result Format

Each rule has a JSON file named `Web-<RuleKey>.json`. The format is:
```json
{
  "project:<relative-file-path>": [<line-numbers>],
  ...
}
```

Example (`Web-S5148.json`):
```json
{
  "project:custom/S5148.html": [1]
}
```

There are 75 expected result files covering both legacy check names (e.g., `ComplexityCheck`)
and S-numbered rules (e.g., `S6853`).

## Dependencies

From `its/ruling/pom.xml`:
- `sonar-orchestrator-junit4` - test infrastructure
- `junit` (JUnit 4)
- `assertj-core` - assertions
- `gson` (2.14.0) - JSON handling
- `sonar-analyzer-commons` - profile generation
- `sonar-ws` - SonarQube web service client

## CI Integration

### Build Workflow (`.github/workflows/build.yml`)

The `ruling` job:
1. **Triggers** after the `build` job succeeds (skipped for dependabot).
2. **Checks out** the repo with submodules enabled.
3. **Runs** `mvn -f its/ruling/pom.xml verify -Pqa -Dsonar.runtimeVersion=LATEST_RELEASE`.
4. **Auto-updates expected results on failure:**
   - Detects if the ruling step failed.
   - Checks the last commit message to prevent infinite loops (skips if message contains
     "Generated with GitHub Actions").
   - Copies `its/ruling/target/actual/*` to `its/ruling/src/test/resources/expected/`.
   - Commits and pushes the updated expected files back to the branch.
5. **Posts a ruling report** as a PR comment:
   - Runs `tools/ruling-report.sh` which compares expected files between the base branch
     and current state.
   - Shows code snippets (5 lines context) for added/removed issues.
   - Limits to 10 snippets per category.
   - Uses collapsible markdown sections.
   - Creates or updates a PR comment with a `<!-- ruling-report -->` marker.

### QA Profile

The `qa` Maven profile (activated by `SONARSOURCE_QA=true`) copies the built plugin JAR
to the expected location before tests run.

## Key Differences from sonar-java

1. **Single test class** - sonar-html has just one `WebRulingTest.java` covering all rules,
   whereas sonar-java has multiple ruling test classes.
2. **LITS plugin** - sonar-html still uses the LITS plugin for comparison, loaded via Maven.
3. **Auto-update mechanism** - The CI automatically commits updated expected results when
   ruling tests fail, then posts a diff report as a PR comment. This is a notable automation
   feature.
4. **Template rules** - Six template rules are instantiated programmatically via the WS API
   during test setup.
5. **ProfileGenerator** - Uses `sonar-analyzer-commons` ProfileGenerator to create a quality
   profile activating all rules, rather than maintaining a static profile XML.
6. **JUnit 4** - Still uses JUnit 4 with `@ClassRule` OrchestratorRule (not JUnit 5).

## Summary

The sonar-html ruling test approach is straightforward:
- One test class runs all rules against a corpus of real-world HTML/JSP files (via git submodule).
- LITS plugin handles the comparison between actual and expected results.
- Expected results are JSON files mapping file paths to line numbers.
- CI auto-updates expected files on failure and posts diff reports on PRs.
- The entire setup is simpler than sonar-java due to having fewer rules (75) and a single
  language family.
