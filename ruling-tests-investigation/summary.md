# Ruling Tests - Cross-Analyzer Summary

## Overview

This report compares the ruling test approaches across all SonarSource analyzer repositories. Ruling tests validate that the analyzer produces the expected set of issues on real-world codebases by comparing actual analysis results against golden reference files (JSON snapshots).

## Classification by Approach

### 1. LITS + Orchestrator (Traditional)

The classic approach: Orchestrator starts a real SonarQube server, installs the analyzer plugin and the LITS plugin, runs SonarScanner, and LITS compares actual issues against expected JSON snapshots.

| Repository | Build System | Test Framework | Profile Generation | Source Management | LITS Version |
|---|---|---|---|---|---|
| sonar-java | Maven | JUnit 4 | ProfileGenerator | Git submodule | 0.11.0.2659 |
| sonar-php | Gradle | — | ProfileGenerator | Git submodule | — |
| SonarJS | Maven | — | ProfileGenerator | Git submodule | — |
| sonar-xml | Maven | — | — | Committed in repo | — |
| sonar-css | Maven | JUnit 4 (vintage) | — | Git submodule | — |
| sonar-ruby | Maven | JUnit 4 (vintage) | — | — | — |
| sonar-apex | Maven | JUnit 4 | — | — | — |
| sonar-cobol | Maven | JUnit 5 | Static profile.xml | Git submodule | 0.11.0.2659 |
| sonar-plsql | Maven | JUnit 5 | ProfileGenerator | Committed in repo | 0.11.0.2659 |
| sonar-tsql | Maven | JUnit 5 | Auto-generated from CheckList | Committed in repo | 0.11.0.2659 |
| sonar-vb | Maven | JUnit 5 | ProfileGenerator | Committed in repo | 0.11.0.2659 |
| sonar-jcl | Maven | JUnit 5 | In-memory XML builder | Git submodule | 0.11.0.2659 |
| sonar-flex | Maven | JUnit 5 | ProfileGenerator | Git submodule | 0.11.0.2659 |
| sonar-html | Maven | JUnit 5 | ProfileGenerator | Git submodule | 0.11.0.2659 |
| sonar-python-enterprise | Maven | JUnit 5 | ProfileGenerator + ProfilesMerger | Git submodule + committed | 0.11.0.2659 |

### 2. SIT-based (Modern, No Server)

Uses `sonar-scanner-integration-tester` (SIT) to run the scanner engine in-process without a real SonarQube server. Some still use LITS as a plugin for issue dumping/comparison.

| Repository | SIT Version | Uses LITS | Issue Comparison | Profile Loading | Source Management |
|---|---|---|---|---|---|
| sonar-kotlin | — | Yes | LITS diff | — | Git submodule |
| sonar-go | — | Yes | Custom Java diff | RulingRules.java (JAR scan) | Git submodules (4) |
| sonar-swift | — | — | Custom Java diff | RulingRules.java (JAR scan) | — |
| sonar-iac / sonar-iac-enterprise | — | Yes | LITS diff | RulingRules.java (JAR scan) | Git submodules + inline |
| sonar-rpg | 0.5.0.1256 | **No** | Custom ExpectedIssues + AssertJ | Static profile.xml | Committed in repo |

### 3. Hybrid (SIT runner + LITS plugin)

Uses SIT as the test runner but loads LITS as a plugin for the issue comparison mechanism.

| Repository | SIT Version | Notes |
|---|---|---|
| sonar-abap | 0.5.0.1256 | Older SIT version |
| sonar-pli | 0.5.0.1256 | Static profile.xml, single test |

### 4. No Traditional Ruling Tests

| Repository | Alternative Approach | Source Management | Issue Comparison |
|---|---|---|---|
| sonar-dotnet | VerifierBuilder + inline assertions | Committed test cases (`analyzers/tests/.../TestCases/`) | Inline `// Noncompliant` comment markers in C#/VB files; Roslyn analyzer verifies diagnostics match |
| sonar-cpp | LLVM lit tests + Orchestrator ITs | Committed (`test/` for lit, `its/projects/` for ITs) | Embedded `CHECK` directives in source files; LLVM `FileCheck` tool matches analyzer output against annotations |
| sonar-text | Specification-based (`AbstractRuleExampleTest`) | Embedded in YAML rule specification files | `@TestFactory` generates tests from YAML `examples:` sections; `PatternMatcher` verifies expected text ranges |
| sonar-dart | PVF (Performance Validation Framework) | Unknown (no local checkout) | A/B comparison between baseline and candidate analyzer versions |
| sonar-rust | Unit tests + e2e Orchestrator | Committed (`e2e/src/test/resources/projects/`) | SonarQube API queries (`wsClient.issues().search()`); AssertJ tuple assertions on line/component/rule |
| sonar-skunk | SIT exporter + diffsit tool | Git submodules (`its/projects/`: Hugo, fzf, NebulaLogger) | JSONL export per analysis; DiffSIT (Rust CLI) compares baseline vs target exports |
| sonar-python | Private/enterprise only | See sonar-python-enterprise | Ruling tests in sonar-python-enterprise (see above) |

## Key Patterns and Observations

### Issue Comparison Approaches

1. **LITS plugin** (most common): Loaded into the analysis engine (either Orchestrator-managed SQ or SIT). Uses scanner properties `sonar.lits.dump.old`, `sonar.lits.dump.new`, `sonar.lits.differences` to compare and produce a diff file. Standard version: `0.11.0.2659`.

2. **Custom Java diff** (sonar-go, sonar-swift, sonar-rpg): Reads issues from `ScannerOutputReader`, groups by rule/file, compares against expected JSON using AssertJ or custom logic. More control over comparison but requires maintaining comparison code.

### Profile Generation Approaches

1. **ProfileGenerator** (sonar-java, sonar-php, SonarJS, sonar-plsql, sonar-vb, sonar-flex, sonar-html, sonar-python-enterprise): From `sonar-analyzer-commons`. Queries the running SQ server for available rules and generates a profile activating all of them with optional parameter overrides. sonar-python-enterprise uses a custom `ProfilesMerger` to combine community and enterprise profiles.

2. **RulingRules.java / JAR scanning** (sonar-go, sonar-swift, sonar-iac): Scans the analyzer JAR for rule-metadata JSON resources matching `(org|com)/sonar/l10n/<language>/rules/<language>/<RuleKey>.json`. No server needed.

3. **Static profile.xml** (sonar-cobol, sonar-pli, sonar-rpg): Hand-maintained XML file listing all rule keys. Simpler but requires manual updates when rules are added.

4. **CheckList annotation scanning** (sonar-tsql): Reads `@Rule(key=...)` annotations from check classes to auto-generate the profile. Coupled to the checks module.

5. **In-memory XML builder** (sonar-jcl): Builds profile XML programmatically from a hardcoded set of rule keys.

### Source Management

| Approach | Repositories |
|---|---|
| Git submodule | sonar-java, sonar-php, SonarJS, sonar-css, sonar-go, sonar-kotlin, sonar-cobol, sonar-jcl, sonar-pli, sonar-flex, sonar-html, sonar-iac, sonar-python-enterprise |
| Committed in repo | sonar-xml, sonar-plsql, sonar-tsql, sonar-vb, sonar-rpg |
| Both (submodule + inline) | sonar-iac-enterprise, sonar-python-enterprise |

### Build Systems

| Build System | Repositories |
|---|---|
| Maven | Most repositories |
| Gradle | sonar-php, sonar-iac/sonar-iac-enterprise |

### Test Frameworks

| Framework | Repositories |
|---|---|
| JUnit 4 | sonar-java, sonar-css, sonar-ruby, sonar-apex, sonar-pli, sonar-rpg |
| JUnit 5 | sonar-cobol, sonar-plsql, sonar-tsql, sonar-vb, sonar-jcl, sonar-iac, sonar-flex, sonar-html, sonar-python-enterprise |

### SonarQube Edition Requirements

| Edition | Repositories |
|---|---|
| Enterprise / Enterprise LW | sonar-jcl, sonar-tsql, sonar-vb, sonar-plsql, sonar-cobol, sonar-abap, sonar-pli, sonar-rpg, sonar-apex, sonar-python-enterprise |
| Developer | sonar-java (some tests) |
| Community / Not needed | sonar-go, sonar-swift, sonar-iac (community test), sonar-rpg |

## Migration Trends

There is a clear trend from the **Orchestrator + LITS** approach toward the **SIT-based** approach:

- **Older analyzers** (sonar-java, SonarJS, sonar-css, sonar-ruby, sonar-apex, sonar-cobol, sonar-plsql, sonar-tsql, sonar-vb, sonar-python-enterprise) still use the traditional Orchestrator + LITS pattern.
- **Newer/modernized analyzers** (sonar-go, sonar-swift, sonar-iac, sonar-kotlin) use SIT, which eliminates the need for a running SonarQube server.
- **Some analyzers** (sonar-abap, sonar-pli) use a hybrid approach with SIT as runner but still rely on LITS for issue comparison.
- **sonar-rpg** is notable for using SIT without LITS at all, replacing it with a custom `ExpectedIssues` class.

### Advantages of SIT over Orchestrator

1. **No SonarQube server needed**: Faster test startup, no license requirements for basic tests
2. **Simpler setup**: No server provisioning, profile upload, or project association
3. **In-process execution**: Easier debugging, no network overhead
4. **Rule discovery from JAR**: No need to query server for available rules

### What remains common

- **LITS plugin** is still widely used even in SIT-based tests (loaded as a scanner plugin)
- **Golden JSON files** are the universal format for expected results
- **Issue format** is consistent: `{"componentKey": [lineNumbers]}` per rule per project
