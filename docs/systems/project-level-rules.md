---
title: Module-wide rules and incremental analysis
description: Aggregate cross-file evidence at module completion without losing or duplicating contributions from unchanged files.
generated:
  by: tirion
---

# Module-wide rules and incremental analysis

## Decide whether the rule needs a module view

`java-frontend/src/main/java/org/sonar/plugins/java/api/internal/EndOfAnalysis.java` runs after file scanning **once per module**, not once per entire project. Use it when the result depends on other files in the same module. Keep normal per-file checks stateless as described in the [stateless rules decision](../adrs/stateless-rules.md).

Retain compact per-file contributions rather than ASTs or semantic symbols. Those objects can retain the entire semantic model. Report module-level issues through `ModuleScannerContext.addIssueOnProject()`. For issues with file locations or secondary spans, see `java-frontend/src/main/java/org/sonar/java/model/DefaultModuleScannerContext.java` and its `reportIssue(AnalyzerMessage)` method.

An IDE analysis can see only a subset of files, so do not treat absence of evidence as proof when a check needs the complete module. A server `ProjectSensor` can run across modules, but is not an IDE callback. `sonar-java-plugin/src/main/java/org/sonar/plugins/java/ProjectEndOfAnalysisSensor.java` sends telemetry; it is not a rule reporting hook.

## Preserve results on unchanged files

`java-frontend/src/main/java/org/sonar/java/model/VisitorsBridge.java` does not simply drop `EndOfAnalysis` checks for unchanged files. Instead it asks them to `scanWithoutParsing()`. The default returns `true` without collecting anything; relying on it means a rule can miss contributions in incremental analysis. Only accept a partial view if the rule remains correct with it.

Use `java-frontend/src/main/java/org/sonar/java/caching/FileCachingCheck.java` instead of wiring cache keys and carry-over by hand:

1. Define a rule-specific `cacheKeyPrefix()` and a compact payload for **one file**.
2. On the parsed path, replace the file's contribution and call `writeToCache()` from `leaveFile()` (or `scanFile()` for a direct scanner). Release per-file traversal state without clearing the module's aggregate.
3. In `scanWithoutParsing()`, return `restoreFromCache(context)`. Its `restore()` callback replaces the contribution under the input file's key, just as the parsed path does.
4. Aggregate the values in `endOfAnalysis()` and report the findings.

A successful restore only means **this** scanner can proceed without the AST. Another scanner can require parsing, so a file may be both restored and visited. Replace, do not append, its contribution on both paths. A cache miss or invalid payload requests parsing; it is not an empty file contribution. An I/O exception reading the cache is handled by the bridge and can fail the analysis in fail-fast mode. If checks share a cache prefix they must share a payload contract: the first write wins.

## Version the payload, not the file identity

`java-frontend/src/main/java/org/sonar/java/serialization/JsonUtils.java` supports a versioned JSON document. Bump the format version when an incompatible payload would otherwise be accepted, so stale entries are parsed and rewritten. Use UTF-8 when converting between JSON and bytes. Prefer explicit Gson `TypeAdapter`s for structured values: reflective binding can break after the plugin's `minimizeJar` step removes classes reached only by reflection.

A span belongs in the payload; the file does not. `java-frontend/src/main/java/org/sonar/java/serialization/TextSpanTypeAdapter.java` encodes spans. Pair them with the file in the restore callback or when aggregating, using the file provided by the scanner context. See `java-frontend/src/main/java/org/sonar/java/model/springcontext/BeanDefinitionGatherer.java` for this pattern.

## Verify the parsed and restored paths

Test full analysis, a mix of unchanged and changed files, a missing or invalid cache entry, and a restore followed by forced parsing. `java-checks/src/test/java/org/sonar/java/checks/DateEnumsCheckTest.java` uses `CheckVerifier.withCache()` and `addFiles(InputFile.Status, ...)` for these cases, including a second scanner that forces parsing. For IDE behavior, do not assume the server analysis cache is available; see [Public API](public-api.md#extension-hooks) for the separate `SonarLintCache` extension.
