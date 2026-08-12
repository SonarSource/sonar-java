# Ruling Tests Investigation: SonarSource/sonar-kotlin

## Overview

The sonar-kotlin repository has recently migrated its ruling tests away from the traditional
Orchestrator + LITS approach to a new **sonar-scanner-integration-tester (SIT)** approach.
The scanner engine runs **in-process** against a mock server, requiring no SonarQube server
or license. Issues are diffed in Java against golden files, replacing the sonar-lits-plugin.

## Repository Structure (Ruling-Related)

```
its/
  sources/                  -> git submodule: SonarSource/slang-test-sources
  ruling/
    build.gradle.kts
    src/integrationTest/
      java/org/sonarsource/kotlin/its/
        KotlinRulingTest.java     # Main ruling test class
        RulingRules.java          # Builds active-rules list from plugin JAR metadata
      resources/
        expected/kotlin/          # Golden files (expected issues)
          android-architecture-components/   # ~62 JSON files per corpus
          corda/
          intellij-rust/
          kotlin/                  # kotlin compiler corpus (CI-only)
          okio/
          test-resources-sources/
        sources/kotlin/            # Small test files (S1144.kt, S2068.kt, etc.)
        junit-platform.properties
  sq-integration/             # Orchestrator-based tests (e.g. language server test)
```

## How It Works

### 1. Test Framework: SIT (sonar-scanner-integration-tester)

The ruling tests use `com.sonarsource.scanner.integrationtester` (SIT), **not** the traditional
Orchestrator + LITS combination. Key dependency in `build.gradle.kts`:

```kotlin
dependencies {
    integrationTestImplementation(testLibs.sit)
    // ...
}
```

SIT version is declared in `settings.gradle.kts` as `sonar-scanner-integration-tester` at
version `1.2.0.1354`.

### 2. No SonarQube Server Required

The `KotlinRulingTest` javadoc explicitly states:

> "Ruling test running the analyzer through the sonar-scanner-integration-tester (SIT) instead of
> the orchestrator: the scanner engine runs in-process against a mock server, so no SonarQube
> server and no license are needed."

A `SonarServerContext` is constructed with mock product/edition settings:
```java
serverContext = SonarServerContext.builder()
    .withProduct(SonarServerContext.Product.SERVER)
    .withServerEdition(SonarServerContext.ServerEdition.ENTERPRISE)
    .withEngineVersion(EngineVersion.latestRelease())
    .withLanguage(LANGUAGE_KEY, "Kotlin", ".kt,.kts")
    .withPlugin(KOTLIN_PLUGIN_LOCATION)
    .withProjectContext(SonarProjectContext.builder().withActiveRules(activeRules).build())
    .build();
```

### 3. Active Rules Loading (RulingRules.java)

Instead of querying a running SonarQube for active rules, `RulingRules.nativeRules()` scans the
plugin JAR's rule metadata directly:
- Reads the plugin JAR file
- Matches rule metadata entries via regex
- Filters out "Sonar_way_profile"
- Returns `List<ActiveRule>` with severity set to INFO
- Supports per-rule parameter overrides (e.g., `S1451` headerFormat)

### 4. Test Corpora

Each `@Test` method analyzes a different open-source Kotlin project corpus:

| Test Method | Corpus | Notes |
|---|---|---|
| `test_kotlin_compiler` | `kotlin` (compiler sources) | CI-only (`@EnabledIfEnvironmentVariable`) |
| `test_resources_sources` | Small `.kt` files in resources | Needs `gradle.main.compile.classpath` |
| `test_kotlin_android` | `android-architecture-components` | |
| `test_kotlin_corda` | `corda` | |
| `test_kotlin_intellij_rust` | `intellij-rust` | |
| `test_kotlin_okio` | `okio` | |

Sources come from the git submodule `its/sources` pointing to `SonarSource/slang-test-sources`.

### 5. Golden File Format

Expected issues are stored as JSON files under `resources/expected/kotlin/<corpus>/`.
Each file is named `kotlin-<ruleId>.json` (e.g., `kotlin-S100.json`).

The JSON format maps component keys to sorted lists of line numbers:
```json
{
  "kotlin-corda-project:sources/kotlin/corda/path/to/File.kt": [12, 45, 78],
  "kotlin-corda-project:sources/kotlin/corda/other/File.kt": [5]
}
```

### 6. Analysis and Diff Process (`analyzeAndAssertDifferences`)

1. **Configure** scanner properties (inclusions, exclusions, performance measures, fail-fast)
2. **Run** `ScannerRunner.run(serverContext, input, config)` -- in-process, no fork
3. **Verify** exit code is 0, no ERROR logs
4. **Group actual issues** by rule key, then by component key, then sorted line numbers
5. **Load expected issues** from golden JSON files
6. **Diff** actual vs expected using multiset comparison (not set -- duplicates on same line matter)
7. **Dump** actual issues to `build/reports/ruling/<corpus>/` for updating golden files
8. **Assert** no differences

### 7. Build Configuration

From `its/ruling/build.gradle.kts`:
```kotlin
tasks.integrationTest {
    dependsOn(":sonar-kotlin-plugin:dist")
    setForkEvery(1)        // Each test in its own forked JVM (leak isolation)
    maxParallelForks = 1   // Sequential (K2/FIR crash avoidance)
    maxHeapSize = "4g"     // Engine runs in-process now, needs more heap
}
```

The `integrationTest` task is enabled via the Gradle property `-Pruling`.

### 8. Updating Golden Files

When actual results differ from expected, the test dumps actual issues to
`build/reports/ruling/<corpus>/`. To update golden files, copy from there:
```
cp -r build/reports/ruling/* src/integrationTest/resources/expected/kotlin/
```

The `-DreportAll=true` flag can be used to generate all actual files regardless of expected state.

## Differences from sonar-java's Approach

| Aspect | sonar-kotlin (SIT) | sonar-java (Orchestrator+LITS) |
|---|---|---|
| Server required | No (mock in-process) | Yes (real SonarQube via Orchestrator) |
| Scanner execution | In-process | Forked scanner process |
| Issue comparison | Java code diffs JSON golden files | LITS plugin on server side |
| License needed | No | Yes (for some editions) |
| Golden file format | JSON (`{component: [lines]}`) | Text-based LITS format |
| Rule activation | Scanned from plugin JAR | Quality profile on server |

## Key Takeaways

1. **sonar-kotlin has moved away from Orchestrator+LITS** to SIT for ruling tests, making them
   faster and simpler (no server startup, no license).
2. The **one exception** is `test_kotlin_language_server`, which still uses Orchestrator in the
   `its/sq-integration` module because it needs a real Gradle build for Java classpath.
3. The golden file diff logic is **self-contained in `KotlinRulingTest.java`** -- about 300 lines
   of Java, no external diff library needed.
4. The SIT library (`sonar-scanner-integration-tester`) is the key enabler -- it provides
   `ScannerRunner`, `SonarServerContext`, and `ScannerInput` APIs.
5. This approach could be a model for migrating sonar-java ruling tests if SIT supports Java
   analysis (which requires more complex classpath/bytecode setup).

## References

- Repository: https://github.com/SonarSource/sonar-kotlin
- SIT library: `com.sonarsource.scanner.integrationtester` (version 1.2.0.1354)
- Test sources submodule: https://github.com/SonarSource/slang-test-sources
- Orchestrator library: https://github.com/SonarSource/orchestrator
- LITS plugin: https://github.com/SonarSource/sonar-lits
