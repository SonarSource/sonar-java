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

To correctly restore state for unchanged files, implement both:

### 1. Write — `leaveFile()`: persist per-file data to cache

```java
@Override
public void leaveFile(JavaFileScannerContext context) {
  super.leaveFile(context);
  if (context.getCacheContext().isCacheEnabled()) {
    var key = CACHE_KEY_PREFIX + context.getInputFile().key();
    var bytes = serialize(myPerFileData);
    try {
      context.getCacheContext().getWriteCache().write(key, bytes);
    } catch (IllegalArgumentException e) {
      LOG.trace("Cache key already written: {}", key);
    }
  }
  myPerFileData.clear();
}
```

### 2. Read — `scanWithoutParsing()`: restore state from cache, skip parsing

```java
@Override
public boolean scanWithoutParsing(InputFileScannerContext context) {
  var key = CACHE_KEY_PREFIX + context.getInputFile().key();
  var bytes = context.getCacheContext().getReadCache().readBytes(key);
  if (bytes != null) {
    context.getCacheContext().getWriteCache().copyFromPrevious(key);
    issues.addAll(deserialize(bytes));  // restore into aggregated state
    return true;   // file does not need to be parsed
  }
  return false;    // cache miss — fall back to full parse
}
```

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
| `SpringBeansShouldBeAccessibleCheck` (S4605) | Full caching: writes packages per file, reads in `scanWithoutParsing`, aggregates, reports in `endOfAnalysis` |
| `BrainMethodCheck` (S6541) | No caching: collects candidates, noise-filters and reports in `endOfAnalysis` |
| `AbstractPackageInfoChecker` | Base class for package-info checks |
| `ExcessiveContentRequestCheck` (S5693) | Cross-file config aggregation |

## Memory warning

> "keeping state between files can lead to memory leaks. Implement with care." — `EndOfAnalysis` javadoc

- Store serializable/primitive data only — never hold AST node references between files
- Clear per-file state in `leaveFile()` (or `setContext()`)