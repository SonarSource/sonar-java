# Ruling Tests Investigation: SonarSource/sonar-python-enterprise

## Overview

The sonar-python-enterprise repository extends the community sonar-python with enterprise-specific
rules and features. Unlike the community edition (which has no publicly visible ruling tests),
the enterprise repository contains a full **Orchestrator + LITS** ruling test infrastructure in
its `private/its-enterprise/ruling/` directory. It uses **JUnit 5** with concurrent execution,
dynamic profile generation via `ProfileGenerator` + a custom `ProfilesMerger` to combine community
and enterprise rule profiles, and tests against 35+ real-world Python projects.

## Repository Structure (Ruling-Related)

```
sonar-python-enterprise/
  its/                                # Open-source integration tests
    commons/                          # Shared IT utilities (PluginLocator, TestsUtils)
    plugin/                           # Plugin-specific IT tests
    pom.xml
  private/                            # Enterprise-only code
    its-enterprise/                   # Enterprise integration tests
      ruling/                         # Ruling tests (LITS)
        pom.xml
        src/test/java/org/sonar/python/it/
          PythonRulingTest.java        # Main ruling test class (35+ test methods)
          FlickeringTest.java          # Non-determinism detection (30-minute loop)
          PythonPrAnalysisTest.java    # PR analysis incremental tests
          ProfilesMerger.java          # Merges community + enterprise profiles
          TestDurationMeasureExtension.java  # Performance monitoring
        src/test/resources/
          expected_ruling/             # Golden files (one JSON per rule)
      it-python-enterprise-plugin/     # Enterprise plugin tests
      sources_ruling/                  # Git submodule -> SonarSource/python-test-sources
      sources_internal_ruling/         # Committed enterprise rule test sources
      sources_internal_namespace_ruling/ # Namespace package test sources
      sources_pr_analysis/             # PR analysis test sources
      pom.xml
    python-enterprise-checks/
    sonar-python-enterprise-plugin/
    pom.xml
  .gitmodules
```

## How It Works

### 1. Test Framework: Orchestrator + LITS (Traditional)

The ruling tests use `OrchestratorExtension` (JUnit 5 variant) with the LITS plugin for
issue comparison. This is the traditional approach, not SIT-based.

Key dependencies in `private/its-enterprise/ruling/pom.xml`:
- `sonar-orchestrator-junit5`
- `sonar-analyzer-commons` (includes `ProfileGenerator`)
- `junit-jupiter` (JUnit 5)
- `sonar-ws` (SonarQube WebServices client)
- `it-commons` (shared IT utilities, type: test-jar)

LITS plugin version: `0.11.0.2659`

### 2. SonarQube Server Required

The tests start a real SonarQube server via Orchestrator:

```java
static OrchestratorExtension getOrchestrator() {
    return getOrchestrator(Edition.ENTERPRISE_LW);
}
```

**Edition:** ENTERPRISE_LW for ruling tests, full ENTERPRISE for PR analysis tests.
License is automatically activated for non-Community editions.

### 3. Profile Generation: ProfileGenerator + ProfilesMerger

Unlike most analyzers that generate a single profile, sonar-python-enterprise uses a **three-step
process** to combine community and enterprise rules:

```java
ProfileGenerator.RulesConfiguration parameters = new ProfileGenerator.RulesConfiguration()
    .add("CommentRegularExpression", "message", "The regular expression matches this comment")
    .add("S1451", "headerFormat", "# Copyright 2004 by Harry Zuzan. All rights reserved.");

// Step 1: Generate community profile
File profileFile = ProfileGenerator.generateProfile(
    serverUrl, "py", "python", parameters, Collections.emptySet());

// Step 2: Generate enterprise profile
File enterpriseProfileFile = ProfileGenerator.generateProfile(
    serverUrl, "py", "pythonenterprise", parameters, Collections.emptySet());

// Step 3: Merge both profiles
File mergedProfile = ProfilesMerger.mergeQualityProfiles(profileFile, enterpriseProfileFile);
```

`ProfilesMerger` is a custom class that:
- Parses both XML profiles using `DocumentBuilder`
- Extracts all rule elements from the enterprise profile
- Imports them into the community profile's `<rules>` element
- Outputs a merged profile for restoration to SonarQube

### 4. Test Corpora

Each `@Test` method analyzes a different real-world Python project from the git submodule
(`SonarSource/python-test-sources`). There are 35+ test methods including:

| Test Method | Corpus | Notes |
|---|---|---|
| `test_airflow` | Apache Airflow | Large project |
| `test_django` | Django web framework | |
| `test_numpy` | NumPy | |
| `test_pandas` | Pandas | |
| `test_scikit_learn` | scikit-learn | |
| `test_tensorflow` | TensorFlow | Largest project |
| `test_celery` | Celery task queue | |
| `test_chalice` | AWS Chalice | |
| `test_tornado` | Tornado web framework | |
| `test_mypy` | MyPy type checker | |
| `test_biopython` | BioPython | |
| `test_calibre` | Calibre e-book manager | |
| `test_libcst` | LibCST compiler toolkit | |

Tests run with `@Execution(ExecutionMode.CONCURRENT)` for parallel execution.

### 5. Golden File Format

Expected issues are stored as JSON files under `src/test/resources/expected_ruling/`.
Each file is named by rule key with a prefix indicating the rule repository:

- `python-S104.json` — community rules
- `pythonenterprise-S7471.json` — enterprise-only rules
- `ipython-S104.json` — Jupyter notebook rules

JSON format maps component keys to sorted lists of line numbers:
```json
{
  "airflow:airflow/cli/cli_parser.py": [0],
  "airflow:airflow/configuration.py": [0],
  "airflow:airflow/models/dag.py": [1066, 1234, 1456]
}
```

### 6. LITS Integration

LITS is configured per-project via SonarScanner properties:

```java
.setProperty("sonar.lits.dump.old", "src/test/resources/expected_ruling/{projectKey}")
.setProperty("sonar.lits.dump.new", "target/actual_ruling/{projectKey}")
.setProperty("sonar.lits.differences", "target/{projectKey}_differences")
```

- **Old dump:** Expected issues from committed JSON golden files
- **New dump:** Actual issues from current analysis
- **Differences:** Written to a file — must be empty for the test to pass

### 7. Source Management

Three types of test sources:

1. **Git submodule** (`private/its-enterprise/sources_ruling/`):
   Points to `SonarSource/python-test-sources` (35+ real-world Python projects: airflow, django,
   numpy, pandas, scikit-learn, tensorflow, etc.)

2. **Committed internal sources** (`private/its-enterprise/sources_internal_ruling/`):
   Small Python files for testing enterprise-specific rules (S7181, S7182, S7187, S7189, S7192,
   S7625 — PySpark, file permissions, SQL injection, hardcoded secrets, XXE).

3. **Namespace test sources** (`private/its-enterprise/sources_internal_namespace_ruling/`):
   Structured test projects for Python namespace packages (basic_namespace, mixed_namespace,
   sam_cloud).

### 8. Build Configuration

From `private/its-enterprise/ruling/pom.xml`:
- **Build Tool:** Maven
- **Java Version:** 17 (source and target)
- **Group:** `com.sonarsource.python` (different from OSS `org.sonarsource.python`)

From `private/its-enterprise/pom.xml`:
- **Maven Surefire:** `skipTests: ${skip.its}`, `forkCount: 1`
- **QA Profile:** Activated when `env.SONARSOURCE_QA=true`

Scanner configuration:
- JVM memory: `-Xmx10000m` for scanner, `-Xmx2000m` for runner
- Analysis threads: `sonar.python.analysis.threads = 4`
- CPD exclusions: `sonar.cpd.exclusions = "**/*"` (no duplication detection in ruling)
- Test heuristic disabled: `sonar.python.testFileHeuristic.disabled = true`

### 9. Additional Test Classes

**FlickeringTest.java:**
- Detects non-deterministic rule behavior
- Runs a 30-minute loop analyzing the same projects repeatedly
- Tests projects: `expected-issues-python`, `pypy`, `sympy`

**PythonPrAnalysisTest.java:**
- Tests PR analysis incremental functionality
- Requires full `Edition.ENTERPRISE` (not ENTERPRISE_LW)
- Test scenarios: newFile, changeInImportedModule, changeInParent, etc.

**TestDurationMeasureExtension.java:**
- JUnit 5 extension measuring test execution time
- Logs the slowest tests for performance monitoring

### 10. CI/CD Pipeline

**build.yml:**
- Triggers on pushes to master/branch-*, PRs
- Git checkout with submodule initialization
- Skips ITs in the build step (`-Dskip.its=true`)
- Ruling tests run separately via QA profile

**ruling-diff-comment.yml:**
- Triggers on PRs modifying `private/its-enterprise/ruling/src/test/resources/expected_ruling/**`
- Posts expected issues diffs as PR comments for review

**FlickeringTest.yml:**
- Detects non-deterministic rule behavior on a schedule

## Comparison with sonar-java

| Aspect | sonar-java | sonar-python-enterprise |
|--------|-----------|-------------------------|
| Ruling approach | Orchestrator + LITS | Orchestrator + LITS |
| Build system | Maven | Maven |
| Test framework | JUnit 4 | JUnit 5 (concurrent) |
| Profile generation | ProfileGenerator | ProfileGenerator + ProfilesMerger (dual profile) |
| LITS version | 0.11.0.2659 | 0.11.0.2659 |
| Test sources | Git submodule | Git submodule + committed internal sources |
| SQ edition | Developer (some tests) | ENTERPRISE_LW (ruling), ENTERPRISE (PR tests) |
| Golden file format | JSON (`{component: [lines]}`) | JSON (`{component: [lines]}`) |
| Parallel execution | No | Yes (`@Execution(CONCURRENT)`) |
| PR analysis tests | Not found | Yes (PythonPrAnalysisTest) |
| Flickering tests | Not found | Yes (FlickeringTest, 30-min loop) |
| Ruling diff CI | Not found | Yes (ruling-diff-comment.yml) |

## Key Takeaways

1. **Traditional Orchestrator + LITS approach**, not SIT-based — requires a real SonarQube server
   with Enterprise license.
2. **Dual profile generation** is unique: community and enterprise rule profiles are generated
   separately and then merged via `ProfilesMerger`. This is different from most analyzers that
   generate a single profile.
3. **Comprehensive test infrastructure** beyond basic ruling: includes flickering detection,
   PR analysis validation, and test duration monitoring.
4. **CI/CD automation** includes automatic PR comments when expected ruling files change, providing
   reviewers with a diff of the issue changes.
5. **JUnit 5 concurrent execution** enables parallel ruling test runs, which is beneficial given
   the 35+ test projects.
6. **Three categories of test sources**: submodule (large OSS projects), committed internal
   (enterprise rule-specific), and namespace packages.