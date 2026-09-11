# Project-Level Rules

Rules that need to aggregate data across **all files in a module** before deciding whether to raise issues.
They run during `endOfAnalysis()` — after every file has been visited.

## Key interfaces

- `EndOfAnalysis` (`java-frontend/.../api/internal/EndOfAnalysis.java`) — callback fired once per module after all files are scanned
- `ModuleScannerContext` — passed to `endOfAnalysis()`; use `addIssueOnProject()` for project-level issues or cast to `DefaultModuleScannerContext` to call `reportIssue(AnalyzerMessage)` for richer issues (secondary locations, text spans)
- `JavaFileScanner.scanWithoutParsing()` — implement to restore state from cache for unchanged files (see Caching section)

## Minimal skeleton

```java
@Rule(key = "SXXXX")
public class MyCheck extends IssuableSubscriptionVisitor implements EndOfAnalysis {

  // State accumulated across files — use lightweight types, NOT AST references
  private final List<AnalyzerMessage> issues = new ArrayList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    // collect candidates into `issues`
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    var defaultContext = (DefaultModuleScannerContext) context;
    issues.forEach(defaultContext::reportIssue);
    // issues list is NOT cleared here — VisitorsBridge creates a fresh check instance per analysis
  }
}
```

## Caching (incremental analysis)

Rules implementing `EndOfAnalysis` are **never skipped** for unchanged files — they are always in `scannersThatCannotBeSkipped`. Without caching, they only see changed files on incremental runs.

Do **not** hand-roll the cache plumbing. Implement `FileCachingCheck<T>` (`java-frontend/.../org/sonar/java/caching/FileCachingCheck.java`), where `T` is the data collected for
one file. It owns the key construction, the `isCacheEnabled()` guard, `copyFromPrevious`, and the error handling — a missing entry, an unreadable entry, or a colliding write are 
all trace-logged and degrade to re-parsing the file.

```java
@Rule(key = "SXXXX")
public class MyCheck extends IssuableSubscriptionVisitor
  implements EndOfAnalysis, FileCachingCheck<MyCheck.PerFileData> {

  private static final String CACHE_KEY_PREFIX = "java:SXXXX:";
  private static final int CACHE_FORMAT_VERSION = 1;
  private static final String COUNT = "count";

  record PerFileData(int count) {}

  @Override
  public String cacheKeyPrefix() {
    return CACHE_KEY_PREFIX;
  }

  @Override
  public byte[] serialize(PerFileData data) {
    var document = JsonCacheFormat.newDocument(CACHE_FORMAT_VERSION);
    document.addProperty(COUNT, data.count());
    return JsonCacheFormat.toBytes(document);
  }

  @Override
  public PerFileData deserialize(InputFile inputFile, byte[] data) {
    var document = JsonCacheFormat.parseDocument(data, CACHE_FORMAT_VERSION);
    return new PerFileData(JsonCacheFormat.requiredInt(document, COUNT));
  }

  @Override
  public void restore(InputFileScannerContext context, PerFileData data) {
    projectCount += data.count();   // merge into the module-level state, as visiting the file would have
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    writeToCache(context, new PerFileData(currentFileCount));
    currentFileCount = 0;
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext context) {
    return restoreFromCache(context);   // true = restored, file needs no parsing
  }
}
```

A check implementing plain `JavaFileScanner` (no `leaveFile`) calls `writeToCache` from `scanFile`
instead.

### Serialization format

Use `JsonCacheFormat` (`org.sonar.java.caching`): versioned JSON documents plus strict accessors that throw on anything unexpected, which `readFromCache` turns into a cache miss. 
Bump your `CACHE_FORMAT_VERSION` whenever the entry shape changes — old entries are then rejected and recomputed.
Locations are covered by `spanToJson`/`spanFromJson`.

Stick to the Gson **tree API**; never its reflective object binding (`new Gson().toJson(pojo)`), because the plugin is shaded with `minimizeJar`.

### Restoring locations

`deserialize` receives the `InputFile` being restored. Anchor every location to it rather than to
anything recorded when the entry was written — see `BeanDefinitionGatherer`.

### Sharing an entry between rules

Two checks may return the same `cacheKeyPrefix()` to share one entry, as `MissingPackageInfoCheck` (S1228) and `UselessPackageInfoCheck` (S4032) do. 
The first write of an analysis wins and the rest are ignored.

### When caching is not needed

If the rule's correctness on incremental runs is acceptable without full history (e.g. noise-reduction rules like `BrainMethodCheck`), you can skip caching entirely. The default `scanWithoutParsing()` returns `true` and the rule simply sees fewer files on incremental runs.

## Behavior by product

| | SQS / SQC | SonarQube for IDE (SonarLint) |
|---|---|---|
| `endOfAnalysis()` fires | Yes, once per module | Yes, but may only see open/active files |
| Incremental file skipping | Enabled (use caching to restore state) | Always file-by-file, no skipping |
| Server-side cache | `context.previousCache()` / `nextCache()` | `SonarLintCache` (in-memory, `isCacheEnabled()` = false) |
| `ProjectSensor` | Runs after all modules | **Not invoked** |

## vs `ProjectSensor`

`EndOfAnalysis` fires per **module**. For truly cross-module aggregation (whole project), use `ProjectSensor` — but it is **not invoked in SonarLint**. The pattern is: accumulate in a shared singleton during each module's `EndOfAnalysis`, then flush/report in `ProjectEndOfAnalysisSensor.execute()`.

Currently `ProjectEndOfAnalysisSensor` only handles telemetry, not rule issues.

## Examples in codebase

| Rule | Pattern |
|---|---|
| `SpringBeansShouldBeAccessibleCheck` (S4605) | Full caching: writes packages per file, aggregates, reports in `endOfAnalysis` |
| `BrainMethodCheck` (S6541) | No caching: collects candidates, noise-filters and reports in `endOfAnalysis` |
| `AbstractPackageInfoChecker` (S1228, S4032) | Two rules sharing one cache entry |
| `ExcessiveContentRequestCheck` (S5693) | Cross-file config aggregation |
| `DateEnumsCheck` (S8694) | Caches potential issues and rebuilds their quick fixes without the AST |
| `BeanDefinitionGatherer` | Frontend gatherer; nested payload, locations re-anchored on restore |

## Memory warning

> "keeping state between files can lead to memory leaks. Implement with care." — `EndOfAnalysis` javadoc

- Store serializable/primitive data only — never hold AST node references between files
- Clear per-file state in `leaveFile()` (or `setContext()`)
