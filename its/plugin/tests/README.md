# Java analysis configuration comparison

Evaluate real source-path resolution (#6308) and analyzer-managed compilation
(#6309) with four independent configurations:

| Mode | Source paths | Internal compilation |
|---|---|---|
| Baseline | Disabled | Disabled |
| Source paths | Enabled | Disabled |
| Bytecode | Disabled | Enabled |
| Combined | Enabled | Enabled |

All modes invoke only `sonar:sonar` through `TestUtils.createMavenBuild()` and
`orchestrator.executeBuild(build)`. No prebuilt target classes are supplied.
Bytecode modes compile temporary classes inside the analyzer and remove them
once analysis finishes. There is no Maven compilation of the target project.

## What the comparison measures

The same 69 sonar-xml production files are copied for every run. Four datasets
separate parser batching from availability of external dependencies:

| Dataset | Parser configuration | External compile dependencies |
|---|---|---|
| Normal / no libraries | 500 KB batches | Absent |
| File by file / no libraries | One file per parser invocation | Absent |
| Normal / dependencies | 500 KB batches | Supplied equally to all modes |
| File by file / dependencies | One file per parser invocation | Supplied equally to all modes |

The normal batching cases retain the ordinary-analysis control. File-by-file
cases deliberately expose resolution across parser boundaries. Feature effects
are comparisons within each dataset; adding dependencies is a separate factor.
All four modes in a dataset share sources, library order/content, JDK, Java 21
language level, Sonar way profile, encoding, and parser settings.

`AnalysisDataset` resolves compile-scope JARs with pinned Maven dependency plugin
`3.8.1:build-classpath` on temporary copies of the parent and module POMs. It
never invokes a compilation lifecycle or modifies sonar-xml. Only external JARs
are accepted; class directories and sonar-xml's own compiled artifact are
rejected. Resolved libraries are copied once and their ordered SHA-256 identity
is recorded. Setup time is recorded separately from scan timings. Normal Maven
repository access is required; cached artifacts can make this setup inexpensive.

## Build the evaluation analyzer

Fresh-fetch both feature branches and the semantic-report target:

```sh
git fetch origin ac/hackathon db/hackathon/optional-compilation alban/SemanticReport
```

Create an isolated evaluation checkout containing those branches. Preserve
source-path configuration, mutable generated-bytecode classpaths, and semantic
reporter module entry/exit/export when resolving their frontend changes. Keep
the production feature implementation outside the test PR.

Apply these reproducible observation patches in order in that checkout:

```sh
git apply /absolute/path/test-checkout/its/plugin/tests/src/test/resources/compilation-measurements.patch
git apply /absolute/path/test-checkout/its/plugin/tests/src/test/resources/semantic-file-measurements.patch
git apply /absolute/path/test-checkout/its/plugin/tests/src/test/resources/semantic-graph-bounds.patch
mvn -pl sonar-java-plugin -am install -Dmaven.test.skip=true
```

- Compilation observations record outcome, duration, source count, and generated
  class count before cleanup without changing compiler arguments.
- Per-file observations add `modules[].files[]` to the latest canonical module
  format, preserving identifier totals and symbol/type metrics.
- The graph guard adds an explicit diagnostic budget. Timed comparisons set
  the budget to **zero**, disabling recursive graph exploration while every
  source identifier is counted and all configured rules and binding probes run.
  Optional `-Dcomparison.graphLimit=1000` enables bounded diagnostic exploration
  in a separate run. JSON records completeness, budget, and expansion count.
  An unrestricted smoke traversal took over seven minutes and was stopped;
  that aborted run is excluded. The retained diagnostic run also contains a
  large timing outlier whose cause was not established, so it is not used to
  rank feature costs.

Commit the evaluation merge and observations for identifiable provenance.
Default JavaSensor timings include direct identifier/per-file observations
but no recursive graph exploration. They are instrumented timings rather than
raw production performance. Diagnostic runs with a positive graph budget must
be interpreted separately. Disabled graph counts are N/A; partial graph counts
are never presented as complete resolution coverage.

Build/install the custom-rule prerequisites from a clean checkout once:

```sh
mvn -pl docs/java-custom-rules-example,its/plugin/plugins/java-extension-plugin -am install -DskipTests
```

After changing the fixture probe, rebuilding its local artifact is sufficient:

```sh
mvn -f its/plugin/plugins/java-extension-plugin/pom.xml package -DskipTests
```

## Run the comparison

```sh
mvn -f its/plugin/tests/pom.xml -Pit-plugin test \
  -Dtest=NoCompilationComparisonTest,AnalysisDatasetTest,AnalysisComparisonReportTest,AnalysisMatrixTest,CompilationMeasurementTest,SourceOnlyComparisonTest,SemanticReportTest,ComparisonSettingsTest \
  -Dcomparison.project="$HOME/Work/Code/sonar-xml" \
  -Dcomparison.pluginJar=/absolute/path/evaluation/sonar-java-plugin/target/sonar-java-plugin-8.45.0-SNAPSHOT.jar \
  -Dcomparison.analyzerCheckout=/absolute/path/evaluation \
  -Dsonar.java.internal.semantic.report=/tmp/report.json
```

The exact scanner flag is also supported on the test command:
`-Dsonar.java.fileByFile=true` selects the two file-by-file datasets, while
`false` selects the two normal-batch datasets. Omitting the flag runs all four.
Within each selected dataset the value is passed identically to Baseline,
Source paths, Bytecode, and Combined; feature modes remain independent.

Maven must be on PATH, or set `-Dmaven.binary=/absolute/path/to/mvn` for both
Orchestrator and dependency resolution. Use your authenticated Maven settings
for repository access. `comparison.pluginJar` can be omitted only when the
local plugin has all features and observations.

The scanner class remains opt-in via `comparison.project`; normal pipeline and
IT runs skip it. Lightweight helper and report tests remain enabled. Each test
invocation starts an isolated Community SonarQube server under
`target/comparison-orchestrator-*` and stops it afterward. Downloads remain
shared and cached. Autoscan, unchanged-file skipping, and test source paths are
disabled. The original checkout and its build outputs are untouched.

Each dataset gets an excluded warm-up per mode and three measured repetitions.
Mode order reverses and rotates between repetitions. Primary identifier counts,
unknown occurrences, findings, compiler outcomes, and class counts must be
stable. `comparison.graphLimit` defaults to 0; budgets from 0 to 1000 are
accepted, and the same budget is applied to every mode. Secondary graph variation
is reported independently when enabled. Add `-Dcomparison.repetitions=1` for one
scan per mode without warm-ups.

An optional UTF-8 file selected by `comparison.candidateProperties` supplies
extra settings shared by every mode. Managed feature flags, input/classpath,
batching, encoding, JDK, output, logging, and credential settings are protected.

## Reviewed correctness checks

Every mode also scans four focused fixtures:

- **Clean compilation:** three JDK-only files analyzed separately. Exact type,
  overload, return-type, and inherited-field probes must match their expected
  bindings. The deprecated `legacy(String)` call at line 8 is a reviewed S1874
  finding; the nondeprecated overload and ordinary call at lines 9–10 must stay
  clean. S1116 reports the empty statement at line 11 in every mode. A missed
  deprecated call is a false negative in this fixture, not a correct zero.
- **Encoding compatibility:** Latin-1 sources contain a non-ASCII method
  identifier. Every mode uses ISO-8859-1; compilation modes must compile the
  sources successfully, exercising #6309's refreshed encoding support.
- **Dependency outside analysis:** valid dependency sources sit outside the
  indexed consumer directory. Source paths must resolve project bindings while
  dependency files remain absent from indexed files and semantic reporting.
  The current compiler receives indexed inputs/classpaths rather than those
  source roots, so internal compilation alone cannot compile the consumer.
- **Compilation failure recovery:** an intentionally missing type causes compiler
  failure. Both files must still be analyzed, expected project bindings remain
  correct, and S1116 still reports. Partial generated bytecode is measured.

Completion markers prevent skipped probes from appearing successful. Bytecode
is checked while analysis runs and must be absent afterward. These profiles are
not active for real-project scans or existing fixtures. Production-source
comparison is implemented here; the feature PRs cover test sources, preview
configuration, and other frontend cases in their own tests.

## Read the report

Each invocation saves only `report.md` beside the test, under the next numbered
`results/run-NNN/` directory. Previous reports survive Maven clean and remain
versioned in Git. The smoke test saves no report or logs.

`AnalysisComparisonReport` puts differences and reviewed findings first, then:

- Identifier totals/known/unknown counts and within-dataset changes for every mode.
- Reviewed TP/FP/FN evidence for S1874 and S1116, separately from addon probes.
- Scan status versus compilation outcome, class output and cleanup.
- JavaSensor min/median/max, change from baseline, compilation time, and remaining
  analysis time derived per sample. Remaining analysis includes parsing,
  checks, direct observations, and any explicitly enabled diagnostic traversal.
  Phase medians need not sum to total medians.
- Observed benefits/costs without claiming a universal winner or statistical
  significance from a few samples.
- Clearly disabled/complete/partial/unknown graph diagnostics and budgets.
- Collapsible fixture and dataset details: per-file semantics, rule counts,
  largest unknown contributors, AST contexts, five feature comparisons, ranked
  changes, and individual identifier/finding differences.
- Source/artifact/classpath hashes, actual feature/reporter revisions, checkout
  provenance, patch identities, versions, JDK, UTC time, and setup cost.

Comparisons require successful scans, identical indexed and semantic file lists,
and identical per-file identifier totals. Invalid or unstable primary results
have no agreement/change metrics. Unknown identifiers match by relative path,
name, and token range. Findings match by rule/path/line while preserving
multiplicity; messages remain available for review. Probe issues are excluded
from product finding counts and agreement metrics. The report is written before
final assertions fail.

Known equals total minus unknown; percentages use aggregate occurrence counts,
not averaged file percentages. Imports, unnamed variables, and pseudo-identifiers
`new` and `class` are excluded. Known symbols alone do not prove correct bindings.
Undefined-type telemetry counts errors rather than coverage. Real-project issue
agreement is not accuracy; only reviewed fixture locations support TP/FP/FN.

Symbol/type counts are N/A when recursive diagnostics are disabled. When
enabled they come from diagnostic graph traversal, not AST occurrences.
Identity-based references can be duplicated when declarations are reparsed, and
more known names can expose additional unknown references. Truncated, variable,
or missing-completeness graph observations suppress location-change
interpretations. None of these graph counts is a correctness percentage.

Server `26.10.0.132816` and Maven scanner `5.9.0.7291` are pinned. Explicit release
versions can override them via `sonar.runtimeVersion` and
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
when using the latest reporter, use the bounded evaluation artifact to avoid
its unrestricted diagnostic traversal. The original surefire-only fixture has
no production sources, so its semantic report can be empty.
