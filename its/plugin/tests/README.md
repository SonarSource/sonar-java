# Java analysis configuration comparison

Compare real source-path resolution (#6308) and analyzer-managed compilation
(#6309) on this **sonar-java** checkout:

| Mode | Source paths | Internal compilation |
|---|---|---|
| Baseline | Disabled | Disabled |
| Source paths | Enabled | Disabled |
| Bytecode | Disabled | Enabled |
| Combined | Enabled | Enabled |

Every mode uses the same analyzer artifact and invokes only `sonar:sonar` through
`TestUtils.createMavenBuild()` and `orchestrator.executeBuild(build)`. No project
classes or project JARs are supplied. Bytecode modes compile temporary classes
inside the analyzer and remove them after analysis.

## Project scope and controls

The current scope contains **1,320 Java files in 11 modules**:

- The ten production modules: `java-frontend`, `java-checks-testkit`,
  `java-checks`, `java-checks-aws`, `java-checks-common`, `check-list`,
  `external-reports`, `sonar-java-plugin`, `java-surefire`, and `java-jsp`.
- The positive two-file `java-checks-test-sources/test-classpath-reader` helper,
  needed by the production testkit APIs.
- IT projects, ruling datasets, negative test fixtures, docs examples, tests,
  generated sources, and existing build outputs are excluded.

Sources are copied from the working tree, including uncommitted production
changes. The report records the checkout state and exact source snapshot hash.
The test preserves the original source files and compiled outputs.

`ReactorProject` creates a dependency-free Maven reactor with the original module
paths. The aggregator has no source roots; each child indexes `src/main/java`.
Source-path modes receive all copied production source roots as absolute paths.
Each module keeps its own analysis scope and external classpath.

| Dataset | Parser configuration | External compile dependencies |
|---|---|---|
| Normal / no libraries | 500 KB batches | Absent |
| File by file / no libraries | One file per parser invocation | Absent |
| Normal / dependencies | 500 KB batches | Supplied equally to all modes |
| File by file / dependencies | One file per parser invocation | Supplied equally to all modes |

Normal batches are the ordinary-analysis control. File-by-file analysis exposes
resolution across parser boundaries. Feature effects are comparisons within a
dataset; adding external libraries is a separate factor. Sources, module scope,
library order/content, JDK, Java 21, profile, encoding, and parser settings remain
identical across each dataset's four modes.

`AnalysisDataset` runs pinned Maven dependency plugin `3.8.1:build-classpath` on
copies of the real reactor POMs, selecting compile scope without compilation or
test execution. The current resolution retains **27 unique external JARs** and
excludes **nine cached project artifacts**. Published `jdt-package` is retained
as an external ECJ dependency. JAR bytes are frozen once with their original
basenames; module-specific versions and classpath order are preserved. Hashes
and dependency-resolution time are recorded separately from scan timings.

Internal compilation sees each module's indexed inputs and classpath; source
roots from #6308 are not forwarded to its compiler. Missing cross-module classes,
`GeneratedCheckList`, or incomplete working-tree edits can therefore produce
FAILED compilation with partial class output while analysis succeeds. The clean
fixtures separately prove successful compilation and correct bindings.

## Build the evaluation analyzer

Fresh-fetch the two feature branches and semantic-report target:

```sh
git fetch origin ac/hackathon db/hackathon/optional-compilation alban/SemanticReport
```

The currently validated feature/reporter revisions are:

- #6308: `7068bcd4c102a0d175db7221785660f685f238ea`.
- #6309: `e019d4b72903e22bee91d66985191335679a059a`.
- Semantic reporter: `9cad3cae7337713c49596b56061ba576b7688aee`.

Merge them in an isolated evaluation checkout. Preserve source-path settings,
generated-bytecode classpaths, and reporter module entry/exit/export when
resolving frontend changes. Production feature code stays outside this test PR.
Apply the observation patches in order, without a feature fallback shim:

```sh
git apply /absolute/path/test-checkout/its/plugin/tests/src/test/resources/compilation-measurements.patch
git apply /absolute/path/test-checkout/its/plugin/tests/src/test/resources/semantic-file-measurements.patch
git apply /absolute/path/test-checkout/its/plugin/tests/src/test/resources/semantic-graph-bounds.patch
mvn -pl sonar-java-plugin -am install -Dmaven.test.skip=true
```

- Compilation measurements record outcome, duration, source count, and generated
  classes before cleanup, preserving compiler arguments.
- Optional `modules[].files[]` observations preserve native module totals and
  allow per-file comparisons. The reader also accepts native module JSON without
  these additional observations; unavailable file data remains unavailable.
- Graph observations have a shared expansion budget. It defaults to **0** for
  timing runs; `-Dcomparison.graphLimit=1000` enables a separate diagnostic run.
  Budgets from 0 to 1000 are supported. Direct identifier counts and rules still
  cover every analyzed source file when recursive diagnostics are disabled.

The earlier seven-minute smoke stall came from an older generic graph traversal.
Reporter revision `9cad3ca` fixes that traversal. Historical diagnostic reports
remain available, including an outlier with an unverified cause; they do not
establish current feature costs. Default timings include direct observations and
measurement hooks, rather than unmodified production performance.

Commit the evaluation merge and observations. Beside the built plugin, an
optional `comparison-analyzer-revisions.properties` records `artifact.sha256`,
`evaluation.revision`, `sourcepath.revision`, `compilation.revision`, and
`semanticReport.revision`. Its artifact hash is validated before using revision
claims. `comparison-analyzer-checkout.txt` can identify the evaluation checkout.
Copy these sidecars with the JAR; the prepared local default artifact includes
them. Missing provenance is reported as not recorded.

Build the custom-rule prerequisites from a clean checkout once:

```sh
mvn -pl docs/java-custom-rules-example,its/plugin/plugins/java-extension-plugin -am install -DskipTests
```

After changing the fixture probe, its local artifact can be rebuilt directly:

```sh
mvn -f its/plugin/plugins/java-extension-plugin/pom.xml package -DskipTests
```

## Run the comparison

Run from the sonar-java checkout:

```sh
mvn -f its/plugin/tests/pom.xml -Pit-plugin test \
  -Dtest=NoCompilationComparisonTest,AnalysisDatasetTest,ReactorProjectTest,AnalysisComparisonReportTest,AnalysisMatrixTest,CompilationMeasurementTest,SourceOnlyComparisonTest,SemanticReportTest,ComparisonSettingsTest \
  -Dcomparison.project="$PWD" \
  -Dsonar.java.internal.semantic.report=/tmp/report.json
```

- `-Dsonar.java.fileByFile=true` selects the two file-by-file datasets.
- `-Dsonar.java.fileByFile=false` selects the two normal-batch datasets.
- Omitting the flag runs both parser configurations. Every selected dataset
  still runs all four feature modes.
- Three measured repetitions follow one excluded warm-up per mode. Mode order
  reverses and rotates. `-Dcomparison.repetitions=1` performs one scan per mode
  without warm-ups.
- `comparison.pluginJar` defaults to the local `sonar-java-plugin/target` JAR.
  To select another evaluation artifact, pass its absolute path and optionally
  `comparison.analyzerCheckout`; adjacent provenance sidecars are also read.
- Maven must be on PATH, or set `-Dmaven.binary=/absolute/path/to/mvn`. Normal
  authenticated repository access is needed for dependency/server downloads;
  artifacts remain cached.

The scanner class is opt-in through `comparison.project` and skipped by normal
pipeline runs. Helper tests remain enabled. Each invocation uses an isolated
Community server under `target/comparison-orchestrator-*` and stops it afterward.
Autoscan, unchanged-file skipping, and test source paths are disabled.

An optional UTF-8 `comparison.candidateProperties` file supplies extra settings
shared by all modes. Managed features, input/classpath, batching, encoding, JDK,
output, logging, and credential settings are protected.

## Reviewed correctness checks

Every mode also scans four fixtures:

- **Clean compilation:** three JDK-only files analyzed separately. Exact type,
  overload, return-type, and inherited-field bindings are checked. S1874 must
  detect the deprecated String overload at line 8 when resolution is enabled;
  nondeprecated calls at lines 9–10 remain clean. S1116 detects line 11 in every
  mode. The baseline's missed deprecated call is a reviewed false negative.
- **Encoding compatibility:** ISO-8859-1 sources contain a non-ASCII identifier.
  Compilation modes must compile successfully, exercising #6309's encoding
  support, with the same reviewed finding controls.
- **Dependency outside analysis:** only the consumer is indexed. Source paths
  resolve the external source declarations without adding dependency files to
  analyzed scope. Internal compilation alone cannot compile those missing inputs.
- **Compilation failure recovery:** an intentionally missing type makes
  compilation fail. Both files remain analyzed, project bindings stay correct,
  and the syntax finding remains present.

Completion markers prove probes ran. Generated bytecode is checked during
analysis and must be removed afterward. These fixture profiles are separate from
real-project Sonar way scans. This comparison covers production sources; the
feature PRs contain their own test-source and other frontend tests.

## Read the report

Each invocation writes only `report.md` beside the test in the next numbered
`results/run-NNN/` directory. Reports remain versioned and survive Maven clean.
The smoke scan writes no report or logs.

The report puts the main comparisons and reviewed findings first:

- Aggregate and module identifier counts, unknown percentages, and changes.
- Reviewed TP/FP/FN evidence, separate from addon probe issues.
- Scan status versus compilation outcome, partial classes, and cleanup.
- JavaSensor min/median/max, compilation duration, and the remaining analysis
  phase derived per sample. Phase medians need not sum to total medians.
- Collapsible module/file, rule, occurrence, and finding comparisons; per-file
  rows appear only when those observations exist.
- Source, analyzer, module-classpath, and patch hashes; revisions, checkout state,
  versions, JDK, time, and setup cost.

Comparisons require successful scans, identical indexed files and module
identifier coverage. When file observations exist, their coverage and per-file
identifier totals must also agree. Invalid or unstable primary measurements have
no change/agreement metrics. Known equals total minus unknown; percentages use
aggregate counts. Imports, unnamed variables, `new`, and `class` are excluded.

Findings match rule/path/line and preserve duplicates. Unknown file occurrences
match path/name/token range when recorded. Probe issues are excluded from product
counts. Real-project agreement is not accuracy; reviewed fixture expectations
support accuracy claims. Fewer unknowns alone do not prove correct bindings.

Graph counts are N/A when disabled. Enabled graph counts describe traversed model
references rather than AST occurrences or coverage percentages. Partial,
variable, or missing-completeness observations suppress location-change
interpretations. A few timing samples do not establish a universal winner.

Server `26.10.0.132816` and Maven scanner `5.9.0.7291` are pinned. Explicit release
versions can override them through `sonar.runtimeVersion` and
`comparison.scannerVersion`; dynamic versions are rejected.

## Raw JSON and original Maven example

Every scan passes its output path with:

```java
.setProperty("sonar.java.internal.semantic.report", semanticReportPath.toString())
```

That property records measurements rather than enabling a feature. By default
JSON stays temporary. The supplied `/tmp/report.json` base writes distinct
`report-<dataset>-<mode>.json` files, plus fixture/warm-up suffixes; repeated
samples overwrite their mode's JSON. IntelliJ can use the same VM property.

`UnitTestsTest.semantic_report_without_compilation` remains beside the original
Maven example. It uses the existing Enterprise lightweight Orchestrator and its
local analyzer artifact, scans three production files, and validates indexed
coverage plus either canonical module JSON or legacy file JSON:

```sh
mvn -f its/plugin/tests/pom.xml -Pit-plugin test \
  '-Dtest=UnitTestsTest#semantic_report_without_compilation' \
  -Dsonar.java.internal.semantic.report=/tmp/report.json
```

That suite needs valid test-license access through `github.token` or
`GITHUB_TOKEN`. The scan-only goal is fully qualified to avoid prefix resolution
issues. Its canonical JSON checks do not require additive per-file observations;
the current reporter also supports its native module format directly. The
original surefire-only fixture has no production sources, so its semantic
report can be empty.
