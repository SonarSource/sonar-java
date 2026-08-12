# Ruling Tests Investigation: SonarSource/sonar-python

## Summary

The sonar-python repository does **not** have a publicly visible ruling test infrastructure in its
open-source (community) codebase. Ruling tests appear to be part of a **private/enterprise module**
that is not accessible in the public repository. The public integration tests (`its/`) focus on
plugin-level behavior (metrics, coverage, custom rules) rather than ruling-style issue verification
against large codebases.

## Repository Structure

```
sonar-python/
  python-frontend/          # Parser and AST
  python-checks/            # Check implementations + unit tests
  python-checks-testkit/    # Test utilities (PythonCheckVerifier)
  python-commons/           # Shared code
  sonar-python-plugin/      # Plugin packaging
  its/                      # Integration tests (public)
    commons/                # Shared IT utilities (TestsUtils, ConcurrentOrchestratorExtension)
    plugin/                 # Plugin integration tests
      it-python-plugin-test/
      python-custom-rules-plugin/
  private/                  # Enterprise module (NOT accessible publicly)
    its-enterprise/
      sources_ruling/       # Git submodule -> SonarSource/python-test-sources
  docs/
```

## Key Findings

### 1. No Public Ruling Tests (No LITS)

Unlike sonar-java, sonar-python does **not** have a public `its/ruling/` directory. There is no
LITS dependency in any accessible POM file. The `its/pom.xml` only declares two modules: `plugin`
and `commons`.

### 2. Ruling Tests Are in a Private Module

The `.gitmodules` file reveals the ruling infrastructure:

```
[submodule "private/its-enterprise/sources_ruling"]
    path = private/its-enterprise/sources_ruling
    url = https://github.com/SonarSource/python-test-sources.git
    branch = master
    shallow = true
```

The `private/` directory returns a 404, confirming it is part of an enterprise/private module
not included in the community edition. The main `pom.xml` has an `its` profile and a `private`
profile (activated when `env.IS_COMMUNITY` is not set).

### 3. Test Source Projects (python-test-sources)

The `SonarSource/python-test-sources` repository is publicly accessible and contains real-world
Python projects used as ruling test inputs:

- **Web frameworks**: Django (2.2.3), Chalice, Tornado
- **Data science**: NumPy, Pandas, TensorFlow, scikit-learn, Keras
- **Infrastructure**: Docker Compose, Salt, Airflow, Celery
- **Dev tools**: MyPy, Black, LibCST
- **Other**: BioPython, NLTK, Calibre, Indico, Timesketch, Buildbot, Saleor, django-shop, django CMS

### 4. Public Integration Tests (its/plugin)

The public `its/` directory contains 17 integration test files that use **Sonar Orchestrator**
(not LITS) to:
- Start a SonarQube instance
- Run SonarScanner on small test projects
- Query the SonarQube API for issues/metrics
- Assert specific expected values (hardcoded in Java)

Test files include:
- `MetricsTest.java` - verifies NCLOC, complexity, duplication metrics
- `CoverageTest.java` - verifies coverage report import
- `CustomRulesTest.java` - verifies custom rule detection
- `TestRulesTest.java` - verifies test file detection and rule scoping
- `SonarLintTest.java` / `SonarLintIPythonTest.java` - SonarLint integration
- `BanditReportTest.java`, `Flake8ReportTest.java`, `PylintReportTest.java`, etc.

### 5. Unit-Level Check Verification

Individual checks are tested using `PythonCheckVerifier` from the `python-checks-testkit` module:

```java
PythonCheckVerifier.verify("src/test/resources/checks/allBranchesAreIdentical.py", check);
```

This uses comment-based annotations in Python test files (similar to sonar-java's approach) to
mark expected issues. The verifier is built on `MultiFileVerifier` from `sonar-analyzer-commons`
(`sonar-analyzer-test-commons` artifact, version 2.29.0.5138).

### 6. CI/CD Pipeline

The `.github/workflows/build.yml` workflow:
- Runs on pushes to `master`, `branch-*`, `dogfood-*`
- Uses `SonarSource/ci-github-actions/build-maven@v1`
- Explicitly skips ITs in the build step: `-Dskip.its=true`
- No separate ruling test job is visible in the public workflows

The `its/Readme.txt` mentions ruling tests require submodule initialization:
```
git submodule init
git submodule update
```

### 7. Orchestrator Configuration

The `TestsUtils.java` configures `ConcurrentOrchestratorExtension` with:
- Dynamic edition selection (Enterprise or Community)
- Multiple quality profile restorations (custom rules, test rules, pylint, nosonar)
- SonarQube version controlled via `sonar.runtimeVersion` property

## Comparison with sonar-java

| Aspect | sonar-java | sonar-python |
|--------|-----------|--------------|
| Public ruling tests | Yes (`its/ruling/`) | No (in `private/` module) |
| LITS dependency | Yes | No (not in public POM) |
| Test sources repo | `sonar-java/java-test-sources` | `SonarSource/python-test-sources` |
| Expected issues files | Public in repo | Not publicly accessible |
| Orchestrator | Yes | Yes |
| Check-level verifier | `CheckVerifier` | `PythonCheckVerifier` (via `MultiFileVerifier`) |
| CI ruling job | Visible | Not visible publicly |

## Conclusion

The sonar-python ruling test infrastructure is hidden behind a private enterprise module. The
mechanism likely follows a similar pattern to sonar-java (analyze large codebases, compare issues
against expected results), but the actual ruling test code, expected issue files, and CI
configuration are not part of the open-source repository. The public `its/` tests focus on
plugin behavior verification with small, purpose-built test projects rather than large-scale
ruling verification.
