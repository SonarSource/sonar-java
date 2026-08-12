# Feasibility Report: Migrating Ruling ITs to `sonar-scanner-integration-tester`

## Context

The sonar-java ruling integration tests (`its/ruling/`) currently use **SonarQube Orchestrator** + **sonar-lits-plugin** to validate issue detection across large open-source projects. This report evaluates whether they can be replaced by the `sonar-scanner-integration-tester` framework (`com.sonarsource.scanner.integrationtester:sonar-scanner-integration-tester:1.1.0.1340`), which runs analysis in-process without a SonarQube server.

## Current Ruling IT Architecture

Each test in `JavaRulingTest.java`:
1. Starts a real SonarQube server (via Orchestrator)
2. Deploys `sonar-java-plugin` + `sonar-lits-plugin`
3. Generates a quality profile with all Java rules activated
4. Compiles the target project with Maven (`clean package sonar:sonar`)
5. LITS plugin captures detected issues and compares against baseline JSON files
6. Test asserts zero differences

## Migration Blockers

### 1. No Maven Build Execution

**Affects: all tests**

The integration tester runs scanner-based analysis on pre-existing source files. It does **not** execute Maven builds. The current ruling tests use `MavenBuild.setCleanPackageSonarGoals()` to compile projects and run analysis in a single step.

This matters because **Java semantic analysis requires compiled bytecode**. Most Java rules depend on type resolution, call graph analysis, and other features that require `.class` files. Without compilation, the analyzer falls back to syntactic-only analysis, which produces different (fewer) issues.

**Workaround**: Pre-compile projects and point the scanner at the compiled classes via `sonar.java.binaries`. The integration tester supports arbitrary scanner properties via `withScannerProperty()`, so `sonar.java.binaries` could be set. However, this means the CI pipeline must compile all ruling projects as a separate step before running the tests.

### 2. No Server State Persistence (Incremental Analysis)

**Affects: `eclipse_jetty_incremental`, `java_time_example_incremental`**

These tests perform **sequential analyses** where later runs depend on state from earlier ones:
- First analysis establishes a main branch baseline
- Subsequent analyses run as PRs against that baseline, with incremental analysis and caching enabled
- The tests assert that PR analysis is faster than full analysis

The integration tester is **stateless** -- each `ScannerRunner.run()` call is independent. There is no way to persist analysis results between runs, which makes branch/PR/incremental testing impossible.

**No workaround available.** These tests fundamentally require a persistent server.

### 3. No SCM/Git Integration

**Affects: `eclipse_jetty_incremental`, `java_time_example_incremental`**

The incremental analysis tests rely on Git SCM integration (`sonar.scm.provider=git`, `sonar.scm.disabled=false`) to determine changed files between branches. The integration tester explicitly disables SCM in all its examples and does not support Git blame or change detection.

**No workaround available.** This is inherent to the stateless design.

### 4. No Enterprise Edition Support

**Affects: `eclipse_jetty_incremental` (requires Enterprise edition with license)**

The incremental analysis test requires `Edition.ENTERPRISE_LW` with license activation. The integration tester supports `SonarServerContext.Product.CLOUD` and `Product.SONARQUBE` but has no concept of editions or license activation.

**No workaround available.**

### 5. LITS Comparison Logic Must Be Rewritten

**Affects: all tests**

The current tests rely on the sonar-lits-plugin to capture issues during analysis and compare them against baseline JSON files. The integration tester has its own result API (`ScannerOutputReader.getProject().getAllIssues()`) which returns structured `Issue` objects.

A new comparison utility would need to be written to:
- Extract issues from `ScannerResultSuccess`
- Map them to the existing JSON baseline format (`{filePath: [lineNumbers]}`)
- Or convert all baselines to a new format

**This is achievable** but represents non-trivial development work, plus a one-time regeneration of all baseline files (since paths and issue formats may differ slightly).

### 6. Quality Profile Generation

**Affects: all tests**

`ProfileGenerator.java` currently fetches all available Java rules from the running SonarQube server via Web API, then activates them in a quality profile. With the integration tester, there is no server to query.

**Workaround**: The sonar-skunk codebase shows a `loadAllRulesForLanguage()` pattern that discovers rules by scanning plugin resources on the filesystem. A similar approach could enumerate all Java rules from the sonar-java-plugin JAR and build `ActiveRule` objects. Template rule instantiation is supported via `ActiveRule.Builder.withTemplateRuleKey()`.

### 7. Performance Measurement

**Affects: performance tracking (non-blocking)**

The current tests set `sonar.java.performance.measure=true` and aggregate results in `PerformanceStatistics.java`. The integration tester has no built-in performance measurement, but manual timing around `ScannerRunner.run()` is possible. The detailed per-rule/per-phase measurements would require the scanner property to still be honored by the plugin when run in-process.

**Likely achievable** since the property is handled by the Java plugin itself, not the server.

## Summary

| Test | Migratable? | Blocking Issues |
|------|-------------|-----------------|
| `guava` | Yes (with pre-compilation) | Needs pre-compiled binaries, LITS rewrite |
| `spring_mall` | Yes (with pre-compilation) | Needs pre-compiled binaries, LITS rewrite |
| `apache_commons_beanutils` | Yes (with pre-compilation) | Needs pre-compiled binaries, LITS rewrite |
| `sonarqube_server` | Yes (with pre-compilation) | Needs pre-compiled binaries, LITS rewrite |
| `regex_examples` | Yes (with pre-compilation) | Needs pre-compiled binaries, LITS rewrite |
| `jboss_ejb3_tutorial` | Yes (easiest) | Uses SonarScanner already, minimal bytecode dependency |
| `vibebot` | Yes (with pre-compilation) | Needs pre-compiled binaries, LITS rewrite |
| `eclipse_jetty_incremental` | **No** | Requires server state, SCM, Enterprise edition, incremental caching |
| `java_time_example_incremental` | **No** | Requires server state, SCM, branch/PR analysis |

## Expected Results Format Comparison

The integration tester produces structured `Issue` objects with rich detail. Here is how its output compares to the formats used across SonarSource analyzers:

| Aspect | sonar-java (LITS) | sonar-python (LITS) | sonar-dotnet (JsonParser) | sonar-cpp | integration-tester |
|--------|-------------------|---------------------|--------------------------|-----------|-------------------|
| **Format** | JSON: `{filePath: [lines]}` | JSON: `{filePath: [lines]}` | JSON: `{Issues: [{Id, Message, Location}]}` | Hardcoded assertions | Java objects (`Issue` hierarchy) / JSONL export |
| **Granularity** | Line numbers only | Line numbers only | Full: message + start/end line + columns | Ad-hoc per test | Full: message + start/end line + column offsets + flows |
| **File organization** | One file per rule per project | One file per rule (all projects) | One file per rule per assembly per TFM | None | Programmatic (or one `.jsonl` for all issues) |
| **Message tracked** | No | No | Yes | Yes (inline) | Yes |
| **Column tracked** | No | No | Yes | No | Yes (start + end offsets) |
| **Data flows** | No | No | No | No | Yes (`Flow` + `FlowLocation`) |
| **Comparison tool** | LITS plugin (server-side) | LITS plugin (server-side) | Custom `ITs.JsonParser` | AssertJ assertions | Custom code needed |

### Implications for migration

The integration tester provides **more information per issue** than LITS (message, exact range, flows). Two strategies for the expected results format:

1. **Keep LITS format** — Write a thin adapter that reduces `TextRangeIssue` objects to `{filePath: [lineNumbers]}` grouped by rule key. This preserves compatibility with existing baselines and the `ruling-update-and-notify` / `ruling-diff-comment` CI actions. No baseline regeneration needed.

2. **Adopt richer format** — Move to a format closer to sonar-dotnet's (with message and location) or to JSONL. This would catch more regressions (message changes, column shifts) but requires regenerating all ~1,200 baseline files and updating the CI diff tooling.

A phased approach is possible: start with option 1 to validate the migration, then optionally enrich the format later.

## Conclusion

**6 out of 8 tests can be migrated**, provided:
1. A pre-compilation step is added to the CI pipeline (or the test setup) to produce `.class` files for each ruling project
2. A LITS-equivalent comparison utility is built on top of the `ScannerOutputReader` API
3. A rule discovery mechanism replaces the current `ProfileGenerator`

**2 tests cannot be migrated** (`eclipse_jetty_incremental` and `java_time_example_incremental`) because they fundamentally depend on stateful server behavior: branch analysis, PR decoration, incremental caching, and SCM integration. These would need to remain on SonarQube Orchestrator or be restructured into a different kind of test.