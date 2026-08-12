# Feasibility Report: Migrating Ruling Tests from Orchestrator+LITS to SIT

## Executive Summary

This report assesses whether sonar-java and other analyzers currently using the traditional
Orchestrator + LITS ruling test approach could migrate to SIT (sonar-scanner-integration-tester).

**Key finding:** SIT replaces the SonarQube server, not the scanner engine. The full scanner
engine runs in-process with all language-specific analyzers intact, including those requiring
bytecode. Scanner properties like `sonar.java.binaries` and `sonar.java.libraries` can be passed
through SIT's `ScannerInput.withScannerProperty()` API. Bytecode-dependent analysis is therefore
**not a blocker**.

Migration is technically feasible for all analyzers currently using Orchestrator + LITS. The main
effort is replacing server-dependent infrastructure (profile generation via REST API, LITS plugin
for issue comparison) with SIT equivalents (rule activation from JAR metadata, in-process issue
diff).

## What SIT Replaces and What It Preserves

| Component | Orchestrator + LITS | SIT |
|-----------|-------------------|-----|
| SonarQube server | Real server instance | Mock/simulated (in-process) |
| Scanner engine | Forked process | In-process (same JVM) |
| Plugin loading | Server installs plugin | Loaded from JAR/Maven location |
| Rule activation | REST API profile upload | `ActiveRule` builder or JAR scanning |
| Issue collection | LITS plugin dumps JSON | `ScannerResultSuccess` API |
| Issue comparison | LITS diff files | Custom Java diff logic |
| Scanner properties | `SonarScanner.setProperty()` | `ScannerInput.withScannerProperty()` |
| Language analysis | Full | Full (unchanged) |
| Bytecode access | Via filesystem | Via filesystem (unchanged) |

**The scanner engine is identical in both approaches.** SIT only changes how the engine is
launched and how results are collected. All analyzer-specific behavior (parsing, semantic
analysis, bytecode analysis, cross-file analysis) is preserved.

## Evidence from Existing SIT Implementations

### Correction: sonar-kotlin Has NOT Migrated to SIT

The initial version of this report stated sonar-kotlin uses SIT. This is incorrect. sonar-kotlin
still uses Orchestrator + LITS (`SlangRulingTest.java` with `OrchestratorExtension`). No SIT
imports exist in its codebase.

### SonarJS (Partial SIT migration)

SonarJS is the best reference for what a SIT migration looks like alongside existing Orchestrator
tests. Its `its/plugin/fast-tests/` module uses SIT for plugin integration tests while the
`its/ruling/` module still uses Orchestrator + LITS for ruling tests.

The SIT helper (`SonarScannerIntegrationHelper.java`) shows the concrete API:

```java
// Rule loading from XML profile (SIT provides a utility)
import static com.sonarsource.scanner.integrationtester.utility.QualityProfileLoader
    .loadActiveRulesFromXmlProfile;

// Rule loading by scanning rule classes via reflection
CssRules.getRuleClasses().stream()
    .map(cssClass -> {
        var key = cssClass.getAnnotation(Rule.class).key();
        return new ActiveRule.Builder()
            .withLanguageKey("css")
            .withName(key)
            .withKey("css", key)
            .withSeverity(ActiveRule.Severity.INFO)
            .withParameters(params)
            .build();
    });

// Server context construction
SonarServerContext.builder()
    .withProduct(SonarServerContext.Product.SERVER)
    .withEngineVersion(EngineVersion.latestRelease())
    .withLanguage("js", "JAVASCRIPT", "sonar.javascript.file.suffixes", ".js,.jsx")
    .withPlugin(pluginLocation)
    .withProjectContext(SonarProjectContext.builder()
        .withActiveRules(activeRules)
        .build())
    .build();

// Analysis execution
ScannerInput build = ScannerInput.create(projectKey, projectDir)
    .withScmDisabled()
    .withVerbose()
    .build();
var result = ScannerRunner.run(serverContext, build, ScannerRunnerConfig.builder().build());

// Issue access
var issues = result.scannerOutputReader().getProject().getAllIssues();
```

### sonar-skunk (Full SIT usage)

Demonstrates arbitrary scanner property passing, confirming that any property from Orchestrator
tests can be forwarded:

```java
ScannerInput.create("project-key", baseDir)
    .withWorkDir(workDir)
    .withScannerProperty("sonar.sensor.cache.enable", "false")
    .withScannerProperty("sonar.cpd.exclusions", "**/*")
    .withScannerProperty("sonar.scm.disabled", "true");
```

## Concrete Migration Plan for sonar-java

### Current Architecture

`JavaRulingTest.java` has 7 test methods:

| Test | Build Type | Key Properties | Special Behavior |
|------|-----------|----------------|------------------|
| `spring_mall()` | MavenBuild | `java.version=21`, `docker.skip=true` | Standard Maven compile+analyze |
| `guava()` | MavenBuild | `sonar.java.source=17`, `batchModeSizeInKB=8192` | Batch mode |
| `apache_commons_beanutils()` | MavenBuild | `batchModeSizeInKB=8192` | Standard |
| `eclipse_jetty_incremental()` | MavenBuild | `sonar.java.binaries` (explicit), `skipUnchanged=true`, `analysisCache.enabled=true`, branch/PR properties | 3 sequential analyses with timing assertions |
| `java_time_example_incremental()` | MavenBuild | Branch/PR properties, `skipUnchanged=true` | 2 sequential analyses testing rule threshold behavior |
| `sonarqube_server()` | MavenBuild | `sonar.java.fileByFile=true` | File-by-file mode |
| `jboss_ejb3_tutorial()` | SonarScanner | `sonar.java.binaries=asynch` (dummy), `sonar.java.source=1.5`, `sonar.java.fileByFile=true` | No Maven build, direct scanner |
| `regex_examples()` | MavenBuild | `sonar.java.fileByFile=true` | Standard |
| `vibebot()` | MavenBuild | — | Vibe-bot test, excluded from CI |

Shared infrastructure:
- `OrchestratorRule` starts a SonarQube server (Community or Enterprise LW)
- `ProfileGenerator` queries the server's REST API for all Java rules, builds XML profile
- `instantiateTemplateRule()` creates custom rule instances via REST API (S2253, S4011, S124)
- `executeBuildWithCommonProperties()` sets LITS properties and runs the build
- `assertNoDifferences()` reads the LITS diff file and asserts it's empty

### Shared Infrastructure Changes

#### 1. Replace ProfileGenerator with JAR-based Rule Loading

The current `ProfileGenerator` queries a running SonarQube at `/api/rules/search` to discover
available rules. With SIT, rules are loaded without a server.

**Option A: Use SIT's built-in XML profile loader**

SIT provides `QualityProfileLoader.loadActiveRulesFromXmlProfile()`. This requires maintaining
a static XML profile file, but avoids any server dependency. The profile could be auto-generated
from the plugin JAR at build time (a Maven plugin or a `@BeforeAll` step).

**Option B: Scan the plugin JAR for rule metadata (like sonar-go/sonar-swift)**

Read rule metadata JSON files from the plugin JAR:
```java
// Pattern: org/sonar/l10n/java/rules/java/<RuleKey>.json
static List<ActiveRule> loadRulesFromJar(Path pluginJar, Set<String> excluded,
    Map<String, Map<String, String>> parameterOverrides) {
    List<ActiveRule> rules = new ArrayList<>();
    try (JarFile jar = new JarFile(pluginJar.toFile())) {
        jar.stream()
            .filter(e -> e.getName().matches("org/sonar/l10n/java/rules/java/S\\d+\\.json"))
            .forEach(entry -> {
                String key = entry.getName().replaceAll(".*/", "").replace(".json", "");
                if (excluded.contains(key)) return;
                var builder = new ActiveRule.Builder()
                    .withLanguageKey("java")
                    .withKey("java", key)
                    .withSeverity(ActiveRule.Severity.INFO);
                if (parameterOverrides.containsKey(key)) {
                    parameterOverrides.get(key).forEach((k, v) ->
                        builder.withParameter(new ActiveRule.Param(k, v)));
                }
                rules.add(builder.build());
            });
    }
    return rules;
}
```

**Option B is recommended** — it mirrors the existing dynamic discovery behavior and doesn't
require maintaining a static file.

#### 2. Handle Template Rule Instantiation

The current test instantiates 3 template rules via REST API:
- `S2253` → `stringToCharArray` (params: className, methodName)
- `S4011` → `longDate` (params: className, argumentTypes)
- `S124` → `commentRegexTest` (params: regularExpression, message)

With SIT, template rule instantiation is done by adding custom `ActiveRule` entries directly:

```java
// Current (REST API):
instantiateTemplateRule("S2253", "stringToCharArray",
    "className=\"java.lang.String\";methodName=\"toCharArray\"", activatedRuleKeys);

// SIT equivalent:
activeRules.add(new ActiveRule.Builder()
    .withLanguageKey("java")
    .withKey("java", "stringToCharArray")
    .withTemplateRuleKey("java", "S2253")
    .withSeverity(ActiveRule.Severity.INFO)
    .withParameters(List.of(
        new ActiveRule.Param("className", "java.lang.String"),
        new ActiveRule.Param("methodName", "toCharArray"),
        new ActiveRule.Param("name", "stringToCharArray"),
        new ActiveRule.Param("key", "stringToCharArray")))
    .build());
```

**Open question:** Does SIT's `ActiveRule.Builder` support `.withTemplateRuleKey()`? The
sonar-skunk `ScannerIntegrationTest.java` uses it for DRE custom rules, suggesting yes. This
needs verification for Java template rules.

#### 3. Replace LITS with In-Process Issue Diff

The current flow:
1. LITS plugin runs inside the SonarQube server during analysis
2. It writes actual issues to `sonar.lits.dump.new`
3. It compares against `sonar.lits.dump.old` (expected golden files)
4. It writes differences to `sonar.lits.differences`
5. Test reads the diff file and asserts it's empty

The SIT flow:
1. `ScannerRunner.run()` returns a `ScannerResultSuccess`
2. Call `result.scannerOutputReader().getProject().getAllIssues()`
3. Group issues by rule key → component key → sorted line numbers
4. Load expected golden JSON files from `src/test/resources/<projectName>/`
5. Diff in Java and report differences

```java
static void assertNoDifferences(ScannerResultSuccess result, String projectName) {
    // Collect actual issues
    Map<String, Map<String, List<Integer>>> actual = new TreeMap<>();
    for (var issue : result.scannerOutputReader().getProject().getAllIssues()) {
        if (issue instanceof TextRangeIssue tri) {
            actual
                .computeIfAbsent("java-" + tri.ruleKey().split(":")[1], k -> new TreeMap<>())
                .computeIfAbsent(tri.componentPath(), k -> new ArrayList<>())
                .add(tri.line());
        }
    }
    // Sort line numbers
    actual.values().forEach(byComponent ->
        byComponent.values().forEach(Collections::sort));

    // Load expected and compare
    Path expectedDir = Path.of("src/test/resources", projectName);
    // ... load each java-*.json file, parse, compare with actual
    // ... report differences
}
```

The golden file format (`{componentKey: [lineNumbers]}`) remains the same. Only the mechanism
for producing actual issues changes.

**Dump for updating:** When differences exist, dump actual issues to
`target/actual/<projectName>/` so developers can review and copy to update expectations.

#### 4. Separate Compilation from Analysis

Currently, `MavenBuild.setCleanPackageSonarGoals()` runs `mvn clean package sonar:sonar` as a
single Orchestrator-managed step. With SIT, these are two distinct operations:

```java
// Step 1: Compile the project (external process)
static void compileProject(Path pomFile, Map<String, String> mavenProperties) {
    var command = new ArrayList<>(List.of("mvn", "-f", pomFile.toString(),
        "clean", "package", "-DskipTests", "-B"));
    mavenProperties.forEach((k, v) -> command.add("-D" + k + "=" + v));
    var process = new ProcessBuilder(command).inheritIO().start();
    assertThat(process.waitFor()).isZero();
}

// Step 2: Analyze with SIT
static ScannerResultSuccess analyzeProject(Path projectDir, String projectKey,
    Map<String, String> scannerProperties) {
    var input = ScannerInput.create(projectKey, projectDir);
    scannerProperties.forEach(input::withScannerProperty);
    var result = ScannerRunner.run(serverContext, input.build(),
        ScannerRunnerConfig.builder().build());
    assertThat(result.exitCode()).isZero();
    return (ScannerResultSuccess) result;
}
```

#### 5. Construct the SonarServerContext

One shared context for all tests:

```java
static SonarServerContext serverContext;

@BeforeAll
static void setup() {
    Path pluginJar = TestClasspathUtils.findModuleJarPath("../../sonar-java-plugin");

    List<ActiveRule> activeRules = loadRulesFromJar(pluginJar,
        Set.of("S1874", "CycleBetweenPackages", "S1106"),
        Map.of(
            "S1120", Map.of("indentationLevel", "4"),
            "S1451", Map.of("headerFormat", "..."),
            "S5961", Map.of("MaximumAssertionNumber", "50"),
            "S6539", Map.of("couplingThreshold", "20")
        ));

    // Add template rule instances
    activeRules.add(templateRule("S2253", "stringToCharArray",
        Map.of("className", "java.lang.String", "methodName", "toCharArray")));
    activeRules.add(templateRule("S4011", "longDate",
        Map.of("className", "java.util.Date", "argumentTypes", "long")));
    activeRules.add(templateRule("S124", "commentRegexTest",
        Map.of("regularExpression", "(?i).*TODO\\(user\\).*", "message", "bad user")));

    serverContext = SonarServerContext.builder()
        .withProduct(SonarServerContext.Product.SERVER)
        .withServerEdition(SonarServerContext.ServerEdition.ENTERPRISE)
        .withEngineVersion(EngineVersion.latestRelease())
        .withLanguage("java", "Java", ".java,.jav")
        .withPlugin(FileLocation.of(pluginJar.toFile()))
        .withProjectContext(SonarProjectContext.builder()
            .withActiveRules(activeRules)
            .build())
        .build();
}
```

### Per-Test Migration

#### `spring_mall()` → straightforward

**Current:** `MavenBuild` with `setCleanPackageSonarGoals()`, custom Maven properties.

**SIT version:**
```java
@Test
void spring_mall() {
    Path projectDir = Path.of("../sources/mall");
    compileProject(projectDir.resolve("pom.xml"), Map.of(
        "docker.skip", "true",
        "java.version", "21",
        "maven-bundle-plugin.version", "5.1.4",
        "maven.javadoc.skip", "true"
    ));

    var result = analyzeProject(projectDir, "com.macro.mall:mall", Map.of(
        "sonar.cpd.exclusions", "**/*",
        "sonar.internal.analysis.failFast", "true",
        "sonar.java.performance.measure", "true",
        "sonar.java.performance.measure.path", perfMeasurePath()
    ));

    assertNoDifferences(result, "mall");
}
```

**Changes:** Maven compilation becomes an explicit `ProcessBuilder` call. Scanner properties
move from `build.setProperty()` to `ScannerInput.withScannerProperty()`. LITS properties
disappear — replaced by in-process diff.

#### `guava()` → straightforward

**Current:** Same as spring_mall but with `sonar.java.source=17` and batch mode.

**SIT version:**
```java
@Test
void guava() {
    Path projectDir = Path.of("../sources/guava");
    compileProject(projectDir.resolve("pom.xml"), Map.of(
        "maven-bundle-plugin.version", "5.1.4",
        "maven.javadoc.skip", "true",
        "animal.sniffer.skip", "true"
    ));

    var result = analyzeProject(projectDir, "com.google.guava:guava", Map.of(
        "sonar.java.source", "17",
        "sonar.java.experimental.batchModeSizeInKB", "8192",
        "sonar.cpd.exclusions", "**/*",
        "sonar.internal.analysis.failFast", "true"
    ));

    assertNoDifferences(result, "guava");
}
```

**Changes:** Identical pattern. `sonar.java.source` and batch mode properties pass through
as scanner properties.

#### `apache_commons_beanutils()` → straightforward

Same pattern as guava. No special considerations.

#### `sonarqube_server()` → straightforward

Same pattern. `sonar.java.fileByFile=true` passes as a scanner property.

#### `regex_examples()` → straightforward

Same pattern. `sonar.java.fileByFile=true` passes as a scanner property.

#### `jboss_ejb3_tutorial()` → straightforward (no compilation needed)

**Current:** Uses `SonarScanner` directly (not `MavenBuild`), no compilation step.
Sets `sonar.java.binaries=asynch` (dummy), `sonar.java.source=1.5`.

**SIT version:**
```java
@Test
void jboss_ejb3_tutorial() {
    Path projectDir = Path.of("../sources/jboss-ejb3-tutorial");

    // No compilation step — this project is analyzed without bytecode
    var result = analyzeProject(projectDir, "jboss-ejb3-tutorial", Map.of(
        "sonar.sources", ".",
        "sonar.java.fileByFile", "true",
        "sonar.java.binaries", "asynch",
        "sonar.java.source", "1.5",
        "sonar.cpd.exclusions", "**/*",
        "sonar.internal.analysis.failFast", "true"
    ));

    assertNoDifferences(result, "jboss-ejb3-tutorial");
}
```

**Changes:** The simplest migration — no MavenBuild to untangle. The `SonarScanner.create()`
call maps directly to `ScannerInput.create()`.

#### `vibebot()` → straightforward

Same pattern as spring_mall. Different pom.xml location (`../vibebot/pom.xml` instead of
`../sources/vibebot/pom.xml`).

#### `eclipse_jetty_incremental()` → cannot migrate to SIT, must stay on Orchestrator

**Current:** Runs 3 sequential Maven builds against the same SonarQube server:
1. Main branch analysis (baseline)
2. Large PR analysis (incremental, expects ~1.25x time of baseline)
3. Small PR analysis (incremental, expects ~0.90x time of baseline)

Uses server-side features: `sonar.branch.name`, `sonar.pullrequest.key`,
`sonar.pullrequest.base`, and analysis cache (`sonar.analysisCache.enabled`).

**SIT cannot support this test.** Investigation of SIT internals confirms:

1. **SIT is stateless.** Each `ScannerRunner.run()` invocation creates an ephemeral context.
   There is no persistent server state between runs — no database, no stored branch analysis,
   no reference branch data. The SIT exporter in sonar-skunk explicitly deletes its work
   directory after each run.

2. **SIT disables caching by default.** The sonar-skunk SIT exporter hardcodes
   `sonar.sensor.cache.enable=false`. Even if enabled via property, there's no server-side
   `ReadCache`/`WriteCache` implementation to persist data between runs.

3. **Branch/PR analysis requires the SonarQube server.** The properties `sonar.branch.name`
   and `sonar.pullrequest.*` are consumed by the SonarQube platform to:
   - Store analysis results for a branch in the server database
   - Compute file change status (`InputFile.Status.SAME`/`CHANGED`/`ADDED`) by comparing
     against the reference branch via SCM integration
   - Provide cached analysis data from the reference branch to the scanner

   Without a real server, these properties have no effect. You can pass them via
   `ScannerInput.withScannerProperty()`, but the scanner engine won't find any reference
   branch data to compare against.

4. **`sonar.java.skipUnchanged` depends on platform state.** The implementation in
   `SonarComponents.java` calls `context.canSkipUnchangedFiles()` (a platform API), then
   checks `inputFile.status() == InputFile.Status.SAME`. The `SAME` status is set by the
   scanner engine's SCM integration, which requires knowing the reference branch's file
   contents — information only available from a server with prior analysis results.

**This test must remain on Orchestrator.** It tests the integration of SonarQube platform
branch analysis with sonar-java's incremental optimization.

**Where this feature is actually implemented:**

The incremental analysis chain involves three layers:

| Layer | Responsibility | Code Location |
|-------|---------------|---------------|
| SonarQube server | Stores branch state, provides reference analysis cache | Platform (not in sonar-java) |
| Scanner engine | SCM integration, sets `InputFile.Status`, provides `ReadCache`/`WriteCache` | Platform (not in sonar-java) |
| sonar-java plugin | Reads `InputFile.Status`, decides which files/rules to skip | `SonarComponents.canSkipUnchangedFiles()` (line 543), `ContentHashCache` |

`sonar.branch.name` and `sonar.pullrequest.*` are handled entirely at the platform level.
sonar-java never reads these properties — it only reads the resulting `InputFile.Status` that
the platform sets based on them.

**Should this test be in sonar-java at all?**

This test validates the *integration* of platform branch analysis with sonar-java's
`skipUnchanged` optimization. It's testing that when the platform says a file is unchanged,
sonar-java correctly skips it and the overall analysis is faster.

This is a valid integration test for sonar-java, because sonar-java implements the skip
logic (deciding which rules to run on unchanged files, managing content hash caching). But
it inherently requires a real SonarQube server with persistent state — it cannot be a
unit test or a SIT test.

#### `java_time_example_incremental()` → cannot migrate to SIT, must stay on Orchestrator

**Current:** Two sequential analyses (main branch, then PR) testing that rule S8694 changes
behavior based on a threshold (85% int literals on main → above threshold → no issues;
50% on PR → below threshold → issues raised).

**Same constraint as `eclipse_jetty_incremental()`:** The PR analysis depends on the scanner
engine knowing the reference branch, which requires server-side state from the prior main
branch analysis.

**What S8694 tests specifically:** S8694 (`DateEnumsCheck`) accumulates cross-file statistics
(percentage of int literals vs enum constants) and makes a threshold decision in
`endOfAnalysis()`. The test verifies that this threshold-based suppression works correctly
across branch/PR boundaries — when the PR changes the ratio, the rule's behavior changes.

This tests the interaction between sonar-java's rule caching (`scanWithoutParsing()`) and
the platform's branch analysis. It must stay on Orchestrator.

### Summary of Changes per Test

| Test | Compilation | SIT Conversion | Risk |
|------|------------|----------------|------|
| `spring_mall` | `ProcessBuilder` mvn call | Direct mapping | None |
| `guava` | `ProcessBuilder` mvn call | Direct mapping | None |
| `apache_commons_beanutils` | `ProcessBuilder` mvn call | Direct mapping | None |
| `sonarqube_server` | `ProcessBuilder` mvn call | Direct mapping | None |
| `regex_examples` | `ProcessBuilder` mvn call | Direct mapping | None |
| `jboss_ejb3_tutorial` | None (no compilation) | Direct mapping | None |
| `vibebot` | `ProcessBuilder` mvn call | Direct mapping | None |
| `eclipse_jetty_incremental` | `ProcessBuilder` mvn call (×3 variants) | **Cannot migrate** — requires server state | Stays on Orchestrator |
| `java_time_example_incremental` | `ProcessBuilder` mvn call (×2 variants) | **Cannot migrate** — requires server state | Stays on Orchestrator |

### What Gets Deleted

- `ProfileGenerator.java` — replaced by JAR-scanning rule loader
- `instantiateTemplateRule()` method — replaced by direct `ActiveRule.Builder` calls
- `prepareProject()` method — no server to provision projects on
- `prepareDumpOldFolder()` / `copyDumpSubset()` — golden files loaded directly by diff logic
- `dumpServerLogs()` — no server logs; SIT logs accessible via `result.logOutput()`
- All LITS properties (`sonar.lits.dump.old`, `.dump.new`, `.differences`)
- LITS plugin dependency in `pom.xml`
- `sonar-orchestrator-junit4` dependency — replaced by SIT dependency
- `sonar-ws` dependency — no REST API calls

### What Gets Added

- `sonar-scanner-integration-tester` dependency
- `RulingRuleLoader` class (~50 lines) — scans plugin JAR for rule metadata
- `RulingIssueDiff` class (~100 lines) — collects issues from `ScannerResultSuccess`, compares
  against golden JSON files, reports differences
- `ProjectCompiler` helper (~30 lines) — wraps Maven compilation via `ProcessBuilder`

### Migration to JUnit 5

The current test uses JUnit 4 (`@ClassRule`, `@BeforeClass`, `@Test` from `org.junit`). Since
SIT uses JUnit 5 APIs, the migration would also upgrade to JUnit 5:

- `@ClassRule OrchestratorRule` → removed (SIT has no rule equivalent)
- `@ClassRule TemporaryFolder` → `@TempDir`
- `@BeforeClass` → `@BeforeAll`
- `@AfterClass` → `@AfterAll`
- `@Test` → `@Test` (from `org.junit.jupiter.api`)

## Per-Analyzer Migration Assessment (Other Analyzers)

### sonar-php

**Current setup:** Orchestrator + LITS, Gradle, ProfileGenerator, git submodule sources.
PHP analysis does not require bytecode. Migration follows the same pattern as the standard
sonar-java tests (minus the compilation step).

**Migration effort: Low.**

### SonarJS

**Current setup:** Orchestrator + LITS for ruling; already has SIT-based fast-tests.
The SIT infrastructure (`SonarScannerIntegrationHelper`) is already built. Ruling tests would
need to adopt the same pattern with golden file diff logic.

**Migration effort: Low.** Most infrastructure exists.

### sonar-css, sonar-ruby, sonar-apex

**Current setup:** Orchestrator + LITS, Maven, JUnit 4.
Simple analyzers without bytecode requirements.

**Migration effort: Low.**

### sonar-cobol, sonar-plsql, sonar-tsql, sonar-vb, sonar-jcl, sonar-flex, sonar-html

**Current setup:** Orchestrator + LITS, Maven, JUnit 5, various profile approaches.
These require Enterprise/Enterprise LW editions. SIT supports edition simulation via
`SonarServerContext.withServerEdition()`. sonar-skunk uses this successfully.

**Migration effort: Low-Medium.** Needs edition simulation validation.

### sonar-python-enterprise

**Current setup:** Orchestrator + LITS, Maven, JUnit 5, dual ProfileGenerator + ProfilesMerger.
With SIT, the `ProfilesMerger` is unnecessary — just scan both plugin JARs and concatenate the
`ActiveRule` lists.

**Migration effort: Medium.** Dual-plugin rule loading, but simpler than current approach.

## Benefits of Migration

### 1. No SonarQube License Required

The most concrete benefit. Currently, ruling tests for sonar-java require Enterprise LW edition
with a valid license. SIT eliminates this, allowing any developer to run ruling tests locally.

### 2. Faster Test Execution

- No server startup overhead (~30-60 seconds saved per test suite)
- In-process execution eliminates network round-trips
- Tests can run in parallel with full isolation

### 3. Simpler CI Configuration

- No SonarQube provisioning step
- No license activation
- No quality profile REST API calls
- No LITS plugin installation
- Fewer moving parts = fewer flaky failures

### 4. Better Developer Experience

- In-process execution enables standard debugger attachment
- Stack traces point directly to analyzer code
- Faster feedback loop during rule development

## Risks and Open Questions

### 1. Branch/PR Analysis Is Not Supported by SIT (Confirmed)

SIT is stateless by design. It has no persistent server state, no branch tracking, and no
analysis cache between runs. The 2 incremental tests (`eclipse_jetty_incremental`,
`java_time_example_incremental`) **must remain on Orchestrator**. This is not a risk — it's
a settled constraint. The remaining 7 tests can migrate to SIT independently.

See the per-test analysis above for the detailed breakdown of why branch/PR properties require
a real SonarQube server (SCM integration for `InputFile.Status`, server-side `ReadCache`/
`WriteCache`, reference branch data).

### 2. Template Rule Instantiation via SIT

The 3 template rules (S2253, S4011, S124) need `.withTemplateRuleKey()` support in SIT.
sonar-skunk uses this API for DRE custom rules, suggesting it works, but Java template rules
might differ.

**Mitigation:** Validate with a single template rule test before full migration.

### 3. LITS Edge Cases

The LITS plugin handles component key normalization and multi-module project key formatting
accumulated over years. The in-process diff logic must produce identical component keys, or the
golden files need regeneration.

**Mitigation:** Run both LITS and SIT in parallel on one project, compare output formats.
If they match, proceed. If not, regenerate golden files once from SIT output.

### 4. Compilation Error Handling

With Orchestrator, `MavenBuild.setCleanPackageSonarGoals()` compiles and analyzes in one step.
A compilation failure is reported by Orchestrator. With SIT, compilation is a separate
`ProcessBuilder` step — its error handling must be explicit (check exit code, capture stderr).

### 5. Golden File Component Key Format

LITS uses the SonarQube project key as part of the component key
(`projectKey:path/to/File.java`). SIT may use a different format for component paths in
`TextRangeIssue.componentPath()`. This affects whether existing golden files can be reused
directly or need regeneration.

**Mitigation:** Compare `TextRangeIssue.componentPath()` output against LITS golden file keys
on one project. If the format matches, all golden files are reusable.

## Recommended Approach

The result is a split architecture: 7 ruling tests on SIT, 2 incremental tests on Orchestrator.

1. **Validate blockers first:** Test SIT with `jboss_ejb3_tutorial` (simplest, no compilation)
   to verify golden file format compatibility.

2. **Validate compilation flow:** Test with `apache_commons_beanutils` (small Maven project)
   to verify the compile-then-analyze two-step works correctly.

3. **Validate template rules:** Test template rule instantiation (S2253) via SIT on any project.

4. **Migrate standard tests:** Once validated, migrate the 7 standard tests (spring_mall,
   guava, commons_beanutils, sonarqube_server, regex_examples, jboss_ejb3_tutorial, vibebot)
   to a new SIT-based `JavaRulingTest` class (JUnit 5).

5. **Keep incremental tests on Orchestrator:** `eclipse_jetty_incremental` and
   `java_time_example_incremental` stay in a separate Orchestrator-based test class. They
   require a real SonarQube server for branch/PR state persistence — this is a fundamental
   SIT limitation, not something that can be worked around.

6. **Simplify Orchestrator infrastructure:** The Orchestrator test class only needs profile
   generation and LITS for the 2 incremental tests. The bulk of the infrastructure
   (ProfileGenerator, LITS) can be simplified but not fully removed as long as these tests
   exist.
