# Source-only analysis comparison

Compare today's SonarJava against a future source-only mode on the production
Java sources of `sonar-xml`. Neither scan compiles the target project or resolves
its dependencies. Both receive only Java sources and the running JDK, with no
`sonar.java.binaries` or `sonar.java.libraries` properties.

The scanner integration tester simulates the server context, so no running
SonarQube instance is needed. Maven must have access to SonarSource's artifact
repository to resolve the integration tester. Use your normal authenticated
Maven settings (`-s /path/to/settings.xml` if necessary).

Build the local analyzer and its required modules once:

```sh
mvn -pl sonar-java-plugin -am install -DskipTests
```

Run the comparison, its scanner smoke test, and the diff tests:

```sh
mvn -f its/scanner-integration-tests/pom.xml test \
  -Dtest=NoCompilationComparisonTest,SourceOnlyComparisonTest \
  -Dcomparison.project="$HOME/Work/Code/sonar-xml"
```

The entire `NoCompilationComparisonTest` class, including its scanner smoke
test, is opt-in through `comparison.project`. Normal pipeline and IT runs skip
both scanner tests. The smoke test verifies that two Java source files can be
analyzed without binaries or libraries. The lightweight diff unit tests remain
part of normal test runs.

Set `CANDIDATE_PROPERTIES` in `NoCompilationComparisonTest` to the scanner
properties enabling the new analyzer once its flag exists. Until then the map is
empty, the report labels the candidate as a placeholder, and the test asserts
identical findings. The candidate uses the same plugin, Sonar way rules, Java 21
language level, JDK, and source files. Autoscan and unchanged-file skipping are
disabled in both modes.

Each scan uses its own temporary directory containing copies of only the
production `.java` files. The original checkout and its build outputs are never
used as an analysis classpath or modified.

Results are stored beside the test in
`its/scanner-integration-tests/src/test/java/org/sonar/java/it/results/`.
Each invocation creates the next numbered directory: `run-001`, `run-002`, etc.
Previous results are preserved, including across Maven clean builds. Maven and
IntelliJ use the same location, and the selected directory is printed when the
class starts. Generated run directories are ignored by Git.

Each run directory contains:

- `report.md`: scan times, status, telemetry, and counts and retention per rule.
- `diff.json`: both runs, full issue details, and current-only/candidate-only findings.
- `current.log` and `candidate.log`: scanner properties, warnings, and failures.

Issues match by rule, repository-relative file path, and primary line, preserving
duplicates. File-level and project-level issues have a null line. Messages,
ranges, and flows remain available in JSON for inspection but do not affect
matching. A failed scan or unexpected indexed file list invalidates agreement
metrics; the test writes the report before failing its assertions.

Retention describes agreement with today's **source-only** findings, not
accuracy. Additional or missing findings need review. Rules without current
findings show `N/A`, and empty result sets are highlighted. Rules can be inactive
in practice when required dependencies are absent; a zero count is not proof
that a rule ran. Unresolved-type telemetry counts particular errors, not semantic
resolution coverage. Times cover scanner execution, including engine setup, and
are single samples rather than a performance benchmark.
