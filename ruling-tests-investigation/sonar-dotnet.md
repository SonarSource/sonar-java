# Ruling Tests Investigation: SonarSource/sonar-dotnet

## Overview

The sonar-dotnet repository uses a **dual ruling test approach**: PowerShell-based regression
tests for the Roslyn analyzers (C# and VB.NET) and Java-based integration tests using the
Orchestrator library. Unlike sonar-java or sonar-kotlin, the ruling tests do **not** use a
SonarQube server or the LITS plugin for the analyzer-level validation. Instead, the Roslyn
analyzers are run locally via PowerShell scripts, and results are compared as JSON files.
The Java ITs (in a separate Maven module) use the Orchestrator for plugin-level integration testing.

**Note:** This investigation was conducted remotely via GitHub web search and web fetch, without
cloning the repository. Some internal details (e.g., exact script content) could not be fully
verified due to repository access constraints.

## Repository Structure (Ruling-Related)

```
analyzers/
  its/                              # Roslyn analyzer integration tests
    regression-test.ps1             # Main ruling test script (PowerShell)
    update-expected.ps1             # Script to update expected results
    expected/                       # Expected issues (JSON, one file per rule)
    actual/                         # Actual issues generated during test run
    sources/                        # Open-source projects analyzed during ruling tests
      ManuallyAddedNoncompliantIssues/   # Hand-crafted C# test project
      ManuallyAddedNoncompliantIssuesVB/ # Hand-crafted VB.NET test project
      Nancy/                        # Nancy web framework
      akka.net/                     # Akka.NET actor framework
      (other OSS projects)
  src/                              # Analyzer source code
  tests/                            # Unit tests (VerifierBuilder-based)
  packaging/                        # NuGet packaging

sonar-csharp-plugin/                # SonarQube plugin for C# (Maven module)
sonar-vbnet-plugin/                 # SonarQube plugin for VB.NET (Maven module)
sonar-dotnet-core/                  # Shared plugin core (Maven module)

azure-pipelines.yml                 # CI pipeline (Azure DevOps)
```

The `dotnet-test-sources` repository (now archived, at `SonarSource/dotnet-test-sources`) previously
held the open-source projects used for ruling tests. It included: AutoMapper, Ember-MM, Nancy,
and akka.net. These sources were later moved or updated as part of issue #3660.

## How It Works

### 1. PowerShell-Based Ruling Tests (Roslyn Analyzer Level)

The primary ruling mechanism is a PowerShell script (`regression-test.ps1`) that runs the
Roslyn analyzers directly against open-source projects and compares the results to stored
expected issues.

**Prerequisites:**
- The analyzer must be built beforehand (debug or release). ITs do not build the analyzer
  themselves; they use the latest build output.
- Must be run from the Developer Command Prompt for VS2022.
- Working directory: `analyzers/its/`

**Running the tests:**
```powershell
# Run all ruling tests
.\regression-test.ps1

# Run for a single rule
.\regression-test.ps1 -ruleId S1234

# Run for a single project
.\regression-test.ps1 -project Nancy
```

**Exit codes:**
- `0`: "SUCCESS: No differences were found!" -- no rules impacted.
- `1`: "ERROR: There are differences between the actual and the expected issues" -- changes
  impacted one or more rules.

### 2. Expected Issues Format

For most projects, expected issues are stored as **JSON files** in the `expected/` directory,
one JSON file per rule. The format maps file paths to lists of line numbers where issues are
expected (same conceptual format as sonar-java and sonar-kotlin golden files).

### 3. ManuallyAddedNoncompliantIssues Projects

Two special projects use an alternative annotation-based approach (similar to unit tests):
- `ManuallyAddedNoncompliantIssues` (C#)
- `ManuallyAddedNoncompliantIssuesVB` (VB.NET)

In these projects, each file tests a single rule. The first occurrence must specify the rule ID:
```csharp
// Noncompliant (S9999)
```
Subsequent occurrences use only:
```csharp
// Noncompliant
```
If multiple rules raise issues in the same file, only the declared rule is verified. The
framework enforces a one-rule-per-file constraint.

### 4. Updating Expected Results

```powershell
.\update-expected.ps1
```

This regenerates the expected issue baselines. All additions, removals, and updates must be
manually reviewed before committing. The diff can be inspected with:
```bash
cd actual
git diff --cached
```

### 5. ITs.JsonParser (Post-2025 Refactoring)

Originally, the scripts used `JavaScriptSerializer` from `System.Web.Script.Serialization`
for JSON parsing (via `create-issue-reports.ps1`). This caused `System.TypeLoadException`
errors when running under PowerShell 7 (which uses .NET 7+ instead of .NET Framework 4.8).

As of April 2025, the codebase was refactored to use an `ITs.JsonParser` utility, eliminating
the `create-issue-reports.ps1` dependency and restoring PowerShell 7 compatibility (issue #6469).

### 6. Java Integration Tests (Plugin Level)

The `its/`, `sonar-csharp-plugin/`, `sonar-dotnet-core/`, and `sonar-vbnet-plugin/` directories
are Maven modules that support Java-based integration tests using the **Orchestrator** library.

**Setup:**
- Requires IntelliJ IDEA with Maven support
- Folders must be imported as Maven modules (look for `pom.xml`)
- Build with: `mvn install clean -DskipTests=true`

**Environment variables:**
- `ORCHESTRATOR_CONFIG_URL`: URI to an `orchestrator.properties` file
  (e.g., `file:///c:/something/orchestrator.properties`)
- `ARTIFACTORY_USER`: repox.jfrog username
- `ARTIFACTORY_PASSWORD`: Identity token for repox.jfrog

These Java ITs test plugin-level features (metric calculation, coverage import, etc.) and
require access to SonarSource internal resources. They are not accessible to external
contributors.

### 7. Peach/Peachee (Production Validation)

Beyond the local ruling tests, SonarSource runs a broader validation pipeline called
**Peach** (or **Peachee**):
- Analyzes a larger set of real-world open-source projects
- Runs on dedicated infrastructure (`peach.sonarsource.com`)
- Configured via separate repositories (`SonarSource/peachee-languages`, `SonarSource/peachee-dotnet`)
- The local ruling tests serve as a pre-Peachee quality gate to catch false positives early

### 8. CI Pipeline

The CI uses **Azure Pipelines** (`azure-pipelines.yml`):
- The main pipeline handles unit tests, code coverage (AltCover), NuGet packaging, and Maven builds
- The ruling/IT tests appear to be run separately, possibly triggered by the
  `/azp run Sonar.Net` command on PRs
- CI is not automatically triggered for external contributor PRs; a team member must
  manually trigger it

## Differences from sonar-java's Approach

| Aspect | sonar-dotnet | sonar-java (Orchestrator+LITS) |
|---|---|---|
| Primary ruling tool | PowerShell scripts (`regression-test.ps1`) | Java test class + Orchestrator + LITS |
| Analyzer execution | Roslyn analyzer runs directly (no server) | SonarQube server analyzes via Orchestrator |
| Server required | No (for analyzer-level ruling) | Yes (real SonarQube instance) |
| License needed | No (for analyzer-level ruling) | Yes |
| Expected format | JSON files (one per rule) | JSON files (one per rule, via LITS) |
| Language of test infra | PowerShell + C# | Java + Maven |
| Rule activation | All rules via ValidationRuleset.ruleset | Quality profile on SonarQube server |
| Plugin-level ITs | Separate Java/Maven module with Orchestrator | Same Orchestrator-based approach |
| LITS usage | Not used | Used for issue comparison |

## Key Takeaways

1. **sonar-dotnet does not use LITS or SIT** for its Roslyn-level ruling tests. Instead, it uses
   custom PowerShell scripts that run the analyzer directly and compare JSON outputs.

2. The **two-tier validation** approach is distinctive: local PowerShell ruling tests catch
   issues early, and Peachee provides broader production validation against many projects.

3. The **ManuallyAddedNoncompliantIssues** projects provide a middle ground between unit tests
   and full ruling tests -- hand-crafted files with known issues, verified per-rule.

4. The **Java-based ITs** using Orchestrator exist but serve a different purpose (plugin-level
   testing) rather than rule-by-rule issue validation.

5. The PowerShell approach is **simpler and faster** than Orchestrator+LITS (no server startup,
   no license) but is tightly coupled to the Windows/.NET ecosystem.

6. Recent refactoring (2025) improved PowerShell 7 compatibility by replacing legacy
   `System.Web` JSON parsing with `ITs.JsonParser`.

7. For sonar-java, the closest equivalent approach would be SIT (as adopted by sonar-kotlin),
   not the PowerShell model, since sonar-java is JVM-based.

## References

- Repository: https://github.com/SonarSource/sonar-dotnet
- Contributing guide: https://github.com/SonarSource/sonar-dotnet/blob/master/docs/contributing-analyzer.md
- Archived test sources: https://github.com/SonarSource/dotnet-test-sources
- Issue #3660 (Update ruling test projects): https://github.com/SonarSource/sonar-dotnet/issues/3660
- Issue #2850 (Add C# 8 ruling tests): https://github.com/SonarSource/sonar-dotnet/issues/2850
- Issue #6469 (PowerShell 7 support): https://github.com/SonarSource/sonar-dotnet/issues/6469
- Orchestrator library: https://github.com/SonarSource/orchestrator
- LITS plugin: https://github.com/SonarSource/sonar-lits
