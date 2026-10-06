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

Run the comparison, its scanner smoke test, and the comparison and semantic-report tests:

```sh
mvn -f its/scanner-integration-tests/pom.xml test \
  -Dtest=NoCompilationComparisonTest,SourceOnlyComparisonTest,SemanticReportTest \
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

Both scans enable the semantic reporter from `alban/SemanticReport` by setting
`sonar.java.internal.semantic.report` to a separate JSON output path. By default
these JSON files are temporary inputs to the comparison, and only the Markdown
report is saved. The test fails if a semantic report is missing, malformed, or
does not contain exactly the intended source files.

To retain the raw semantic JSON files, add this JVM/Maven property:

```text
-Dsonar.java.internal.semantic.report=/tmp/report.json
```

The comparison writes `/tmp/report-current.json` and `/tmp/report-candidate.json`
and prints each path. These files are overwritten on the next invocation; the
smoke test continues to use its temporary workspace. The same property can be
set in IntelliJ's test run configuration VM options. It selects the output path,
not the candidate analyzer mode.

Each scan uses its own temporary directory containing copies of only the
production `.java` files. The original checkout and its build outputs are never
used as an analysis classpath or modified.

Results are stored beside the test in
`its/scanner-integration-tests/src/test/java/org/sonar/java/it/results/`.
Each invocation creates the next numbered directory: `run-001`, `run-002`, etc.
Previous results are preserved, including across Maven clean builds. Maven and
IntelliJ use the same location, and the selected directory is printed when the
class starts. Keep saved run directories in Git so results can be reviewed
alongside the test.

Each run directory contains only `report.md`. Its summary shows scan status,
file and finding counts, total/known/unknown identifiers, unknown percentages,
timings, error telemetry, shared and differing findings, and retention. A
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
A failed scan or unexpected indexed file list invalidates agreement
metrics; the test writes the report before failing its assertions.

Retention describes agreement with today's **source-only** findings, not
accuracy. Additional or missing findings need review. Rules without current
findings show `N/A`, and empty result sets are highlighted. Rules can be inactive
in practice when required dependencies are absent; a zero count is not proof
that a rule ran. Unresolved-type telemetry counts particular errors, not semantic
resolution coverage. Times cover scanner execution, including engine setup, and
are single samples rather than a performance benchmark.

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
