# Java semantic model telemetry

These counters describe observed semantic degradation and possible configuration causes. They do not classify a project as misconfigured automatically. All counters aggregate across modules and contain no source paths, type names, or diagnostic messages.

## Semantic observations

The prefix `java.analysis.<scope>.semantic.` applies separately to `main`, `test`, and `generated` sources.

| Suffix | Meaning |
| --- | --- |
| `files_evaluated` | Files for which parsing and AST conversion returned a semantic model and its diagnostics were examined. |
| `files_with_resolution_errors` | Evaluated files with at least one `UNDEFINED_TYPE` diagnostic. |
| `resolution_error_count` | Sum of `UNDEFINED_TYPE` diagnostics, deduplicated by message within each file. Preview-feature diagnostics are excluded. |
| `modules_evaluated` | Modules with at least one evaluated file in this scope. |
| `modules_with_resolution_errors` | Modules with at least one affected file in this scope. |

Observations are recorded before rule execution, so a later visitor failure does not erase the semantic observation. The existing `success.type_error_count` remains unchanged, including its preview-feature diagnostics and restriction to successful analysis.

The affected-file ratio is `files_with_resolution_errors / files_evaluated`. An absent or zero denominator means unobserved. Zero resolution errors means no degradation was detected by these diagnostics; it does not prove that every binding resolved correctly. No explicit model-absence counter is emitted because normal Java parsing constructs a semantic model even with an incomplete classpath.

## Analysis coverage

Under `java.analysis.<scope>.`, the counters `success.file_count`, `parse_errors.file_count`, and `exceptions.file_count` count file processing outcomes, alongside the existing size and time measurements. `cached_file_count` applies only to `main` and `test`: it counts files successfully processed without parsing through server-side caching and is emitted when that optimization is attempted. Generated sources always go through parsing and do not emit this counter. Cached files have no new semantic observation. A parse failure has no semantic observation either. An exception may occur before or after semantic observation, so these counters must not be added to `files_evaluated` as disjoint populations.

## Configuration signals

The prefix `java.classpath.<scope>.` applies separately to `main` and `test`. Only modules containing Java sources in the corresponding scope contribute.

| Suffix | Meaning |
| --- | --- |
| `module_count` | Modules whose classpath configuration was examined. |
| `modules_missing_binaries` | Modules without the corresponding binaries property. |
| `modules_without_binary_entries` | Modules whose binaries property resolves to no entries. |
| `modules_missing_libraries` | Modules without the corresponding libraries property. |
| `modules_without_library_entries` | Modules whose libraries property resolves to no entries, excluding JDK entries. |
| `modules_with_unmatched_entries` | Modules with a configured binaries or libraries pattern that produces the existing unmatched-entry warning, including partial classpaths with other valid entries. |
| `modules_with_invalid_jdk` | Modules whose configured `sonar.java.jdkHome` path is missing or is not a directory, causing fallback to the runtime JDK. |

These signals are collected for AutoScan too; existing warning suppression is unchanged. Missing libraries may be legitimate for JDK-only code. A resolved directory is an entry even if empty; these metrics do not inspect bytecode contents or establish freshness. Main and test configuration counters overlap when both scopes exist, so their module counts must not be summed to obtain distinct project modules.

Use affected-file and affected-module ratios to measure degradation, then correlate with configuration signals and existing scanner, AutoScan, language-version, and analyzer-version context. Project aggregates cannot prove that a configuration problem and a resolution error occurred in the same module. Track distinct projects using their latest analysis and distinguish full observations from partial or cache-only observations.

Invalid classpath entries that abort analysis, binding-level completeness, diagnostic categories beyond the existing parser diagnostics, and telemetry for failed scanner executions are outside this first iteration. The existing project-end sensor submits these counters on completed analyses.
