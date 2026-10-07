# Source-only analysis comparison

Compare today's SonarJava against a future source-only mode on the production
Java sources of `sonar-xml`. Neither scan compiles the target project or resolves
its dependencies. Both receive only Java sources and the running JDK, with no
project binaries or dependency libraries.

The comparison uses the same `TestUtils.createMavenBuild()` and
`orchestrator.executeBuild(build)` runner as `UnitTestsTest`. Orchestrator starts
a temporary Community Edition SonarQube server with the local Java plugin and
stops it after the tests. Maven and Orchestrator need access to their artifact
repositories for the test dependencies, scanner, and server distribution. Use
your normal authenticated Maven settings (`-s /path/to/settings.xml` if needed).
The comparison gives each invocation a unique Orchestrator workspace under
`target/comparison-orchestrator-*`, isolating it from the original suite and
other comparison invocations. Downloaded artifacts remain shared and cached.

Build the local analyzer and the custom-rule artifact required by this IT module once:

```sh
mvn -pl sonar-java-plugin,docs/java-custom-rules-example,its/plugin/plugins/java-extension-plugin -am install -DskipTests
```

Run the comparison, its scanner smoke test, and the comparison and semantic-report tests:
Maven must be on PATH, or supply `-Dmaven.binary=/absolute/path/to/mvn` for the
Orchestrator Maven runner.

```sh
mvn -f its/plugin/tests/pom.xml -Pit-plugin test \
  -Dtest=NoCompilationComparisonTest,SourceOnlyComparisonTest,SemanticReportTest,ComparisonSettingsTest \
  -Dcomparison.project="$HOME/Work/Code/sonar-xml"
```

The entire `NoCompilationComparisonTest` class, including its scanner smoke
test, is opt-in through `comparison.project`. Normal pipeline and IT runs skip
both scanner tests. The smoke test verifies that two Java source files can be
analyzed without binaries or libraries. The lightweight diff unit tests remain
part of normal test runs.

Provide `-Dcomparison.candidateProperties=/absolute/path/candidate.properties`
to load UTF-8 scanner properties enabling the new analyzer. Put the actual
feature flag supported by your analyzer build in that file. Shared source, JDK,
classpath, reporting, and logging settings cannot be overridden there. With no
file, or an empty file, the report labels the candidate as a placeholder and
requires identical findings and semantic counts. The candidate uses the same plugin, Sonar way rules, Java 21
language level, JDK, and source files. Autoscan and unchanged-file skipping are
disabled in both modes.

The Maven build passes the report path with
`.setProperty("sonar.java.internal.semantic.report", semanticReportPath.toString())`.
This is equivalent to passing `-Dsonar.java.internal.semantic.report=/tmp/report.json`
to the Maven analysis command.

Both scans enable the semantic reporter from `alban/SemanticReport` by setting
`sonar.java.internal.semantic.report` to a separate JSON output path. By default
these JSON files are temporary inputs to the comparison, and only the Markdown
report is saved. The test fails if a semantic report is missing, malformed, or
does not contain exactly the intended source files.

To retain the raw semantic JSON files, add this JVM/Maven property:

```text
-Dsonar.java.internal.semantic.report=/tmp/report.json
```

The measured scans write `/tmp/report-current.json` and
`/tmp/report-candidate.json` and print each path. Binding fixtures and warm-ups
use separate `report-bindings-*` and `report-warmup-*` filenames. These files are overwritten on the next invocation; the
smoke test continues to use its temporary workspace. The same property can be
set in IntelliJ's test run configuration VM options. It selects the output path,
not the candidate analyzer mode.

Each scan uses its own temporary directory containing copies of only the
production `.java` files and a generated minimal POM without project
dependencies. `MavenBuild` runs only `sonar:sonar`: it never runs `compile`,
`test-compile`, or tests for the target project. Empty `sonar.java.binaries` and
`sonar.java.libraries` override Maven scanner defaults. The original checkout
and its build outputs are never used as an analysis classpath or modified.

Results are stored beside the test in
`its/plugin/tests/src/test/java/com/sonar/it/java/suite/results/`.
Each invocation creates the next numbered directory: `run-001`, `run-002`, etc.
Previous results are preserved, including across Maven clean builds. Maven and
IntelliJ use the same location, and the selected directory is printed when the
class starts. Keep saved run directories in Git so results can be reviewed
alongside the test.

Each run directory contains only `report.md`. Its summary shows scan status,
file and finding counts, total/known/unknown identifiers, unknown percentages,
timings, error telemetry, shared and differing findings, and retention. Run metadata records UTC time, checkout revisions and dirty
state, source and plugin SHA-256 hashes, JDK, pinned versions, candidate settings,
correctness results, and the timing protocol. Ranked improvements and regressions
precede the full per-file table. A
per-file table compares identifier counts and the change in unknown percentage.
The summary also reports files with no unknown identifiers, improved/unchanged/
regressed file counts, and the net change in unknown identifiers. A top-five
table highlights the largest contributors to unknown identifiers in either run.
The rule table includes only rules with findings; the count of
rules with no findings is summarized separately. Finding locations and messages
are included below the tables. Scanner failure diagnostics appear in the report
when needed. The smoke test does not save any artifacts.

Issues match by rule, repository-relative file path, and primary line, preserving
duplicates. File-level and project-level issues have no line, shown as `N/A`.
Messages are included for review but do not affect matching.
A failed scan, unexpected indexed or semantic file list, or differing identifier
totals for any file invalidates agreement metrics; the test writes the report before failing its assertions.

Retention describes agreement with today's **source-only** findings, not
accuracy. Additional or missing findings need review. Rules without current
findings show `N/A`, and empty result sets are highlighted. Rules can be inactive
in practice when required dependencies are absent; a zero count is not proof
that a rule ran. Unresolved-type telemetry counts particular errors, not semantic
resolution coverage. Times cover scanner execution, including engine setup, and
include both whole Maven/server wall time and JavaSensor-only execution time
(parsing, semantics, checks, and metrics). By default, each mode gets one
unmeasured warm-up and three measured repetitions; pair order alternates. The
report lists individual samples and medians. `-Dcomparison.repetitions=1` runs
a quick single pair without warm-ups. All repetitions must produce stable
findings and semantics. Server startup is excluded, and no CI speed threshold
is enforced.

Semantic metrics count identifier occurrences, excluding unnamed variables,
whose symbols the analyzer marks known or unknown. Known identifiers are total
minus unknown. The global unknown percentage uses the aggregate counts rather
than averaging file percentages; files with no identifiers show `N/A`.
Percentage-point changes are candidate minus current, so negative values mean
fewer unresolved identifiers. A known symbol is not proof of fully correct
semantic resolution, and these metrics do not count distinct object properties.
Files with no unknown identifiers must have at least one identifier. Files with
zero identifiers in either run have no comparable percentage and are reported
separately from improved, unchanged, and regressed files.

## Small test beside the original Maven example

`UnitTestsTest.semantic_report_without_compilation` uses the existing
`JavaTestSuite.ORCHESTRATOR` and `TestUtils.createMavenBuild()` directly. It scans
the existing `measures-on-directory` fixture without compilation and checks that
the requested JSON report contains all three production files. It is opt-in via
the semantic-report output property.

```sh
mvn -f its/plugin/tests/pom.xml -Pit-plugin test \
  '-Dtest=UnitTestsTest#semantic_report_without_compilation' \
  -Dsonar.java.internal.semantic.report=/tmp/report.json
```

This existing suite uses the Enterprise lightweight server and needs a GitHub
token with access to SonarSource's test-license repository (`github.token` in
Orchestrator configuration or `GITHUB_TOKEN`). The test uses the fully qualified
Sonar Maven goal so it does not depend on shorthand plugin-prefix resolution.

The original `tests-surefire-suffix` fixture contains only test sources. The
semantic reporter currently counts production sources, so its report can be
empty; the small semantic test deliberately uses a fixture with main sources.

## Reproducibility and binding correctness

The comparison pins Community server `26.10.0.132816` and Maven scanner
`5.9.0.7291`. Override them with explicit release versions using
`-Dsonar.runtimeVersion=...` and `-Dcomparison.scannerVersion=...`; dynamic
versions such as `LATEST_RELEASE` are rejected. Language level remains Java 21,
while the report records the actual test JDK. The analyzer plugin itself is your
locally built development artifact; its version and SHA-256 identify it exactly.

A dedicated `semantic-bindings` profile activates an IT-only binding probe in
the extension plugin. Before the project comparison, both configurations analyze
a two-file golden fixture without compilation. The probe checks the exact
`String` type, a cross-file helper type, overloaded `select(String)` method and
return type, inherited field owner/type, and intentionally unresolved
`MissingType`. It emits a completion marker so skipped probes cannot look like
success. All five checks must run and match the expectations; mismatch details
are included in report metadata before assertions fail. This profile is not
active for the real-project scans or existing fixtures.
