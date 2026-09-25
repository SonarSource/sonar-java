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
one file. It owns the key construction, the `isCacheEnabled()` guard, `copyFromPrevious`, and the error handling.

No cache problem is fatal, but they degrade differently — worth knowing when a trace log is all you have to debug a miss:

| Problem | Logged? | Consequence |
|---|---|---|
| Entry cannot be deserialized | trace | file is parsed instead, and the entry rewritten |
| No entry for the file | no | file is parsed instead |
| Write collides with an earlier one | trace | new payload dropped, earlier entry kept; the file had been parsed either way |
| `copyFromPrevious` collides | trace | none — the data stays restored; another check sharing the entry already copied it |
| I/O failure reading the cache | warn, by `VisitorsBridge` | file is parsed instead; fails the analysis only in fail-fast mode |

Returning `true` from `restoreFromCache` only means that one check does not require parsing. The file is
skipped only when every scanner that cannot be skipped returns `true`. Another scanner's cache miss or
failure can therefore cause the check to visit a file after restoring its cached contribution. Keep
module-level state keyed by `context.getInputFile().key()`, and replace that file's contribution from both
the restored and parsed paths.

```java
@Rule(key = "SXXXX")
public class MyCheck extends IssuableSubscriptionVisitor
  implements EndOfAnalysis, FileCachingCheck<MyCheck.PerFileData> {

  private static final String CACHE_KEY_PREFIX = "java:SXXXX:";
  private static final int CACHE_FORMAT_VERSION = 1;
  private static final String COUNT = "count";

  private final Map<String, PerFileData> dataByFile = new HashMap<>();
  private int currentFileCount;

  record PerFileData(int count) {}

  @Override
  public String cacheKeyPrefix() {
    return CACHE_KEY_PREFIX;
  }

  @Override
  public byte[] serialize(PerFileData data) {
    var document = JsonUtils.writeDocument(CACHE_FORMAT_VERSION,
      out -> out.name(COUNT).value(data.count()));
    return document.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public PerFileData deserialize(byte[] data) {
    var document = JsonUtils.parseDocument(new String(data, StandardCharsets.UTF_8), CACHE_FORMAT_VERSION);
    return new PerFileData(JsonUtils.requiredInt(document, COUNT));
  }

  @Override
  public void restore(InputFileScannerContext context, PerFileData data) {
    dataByFile.put(context.getInputFile().key(), data);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    var data = new PerFileData(currentFileCount);
    dataByFile.put(context.getInputFile().key(), data);
    writeToCache(context, data);
    currentFileCount = 0;
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext context) {
    return restoreFromCache(context);
  }
}
```

Aggregate `dataByFile.values()` in `endOfAnalysis`. Recording the parsed value before clearing per-file
state is what replaces an earlier restore when another scanner forces parsing.

A check implementing plain `JavaFileScanner` (no `leaveFile`) calls `writeToCache` from `scanFile`
instead.

### Serialization format

Wrap the entry in a versioned document with `JsonUtils` (`org.sonar.java.serialization`):
`writeDocument(version, body)` returns the document as a `String`, `parseDocument(content, version)`
reads it back, and the check converts to and from `byte[]` in UTF-8 itself. Bump your
`CACHE_FORMAT_VERSION` whenever the entry shape changes — old entries are then rejected and
recomputed. Every `JsonUtils` accessor is strict and throws on anything unexpected, which
`readFromCache` turns into a cache miss.

Several checks sharing one format keep it in a dedicated class rather than in each check — see
`SpringContextCacheHelper`, which owns the version the Spring gatherers share along with the
`byte[]` conversions.

Describe anything with structure as a Gson **`TypeAdapter`** rather than assembling the tree by hand —
see `BeanDefinitionHolderTypeAdapter` and `InjectionPointTypeAdapter`, which read with `JsonUtils.readString`,
`readNullableString` and `readStrings` (`readInt` too, in `TextSpanTypeAdapter`). Adapters should skip
unknown properties (`default -> in.skipValue()`) so a newer entry shape stays readable.

Never use Gson's reflective object binding (`new Gson().toJson(pojo)`): the plugin is shaded with
`minimizeJar`, which strips classes reached only by reflection. Explicit `TypeAdapter`s and the tree
API are both safe.

### Restoring locations

`TextSpanTypeAdapter.getInstance()` (`org.sonar.java.serialization`) serializes an `AnalyzerMessage.TextSpan`.

A payload holds **text spans only, never the file they belong to**. File identity lives in the cache key
(`cacheKeyPrefix() + inputFile.key()`), which is what lets `deserialize(byte[])` read an entry without
knowing which file it describes. Name a per-file record after that property — see
`BeanDefinitionHolder.InputFileData`.

`restore(InputFileScannerContext, T)` is the only place the file reaches the check, so store the data
under it and pair the spans with it when aggregating in `endOfAnalysis`. `BeanDefinitionGatherer` keys
`beansCollectedByFile` by `InputFile` and builds every `BeanLocation(inputFile, data.textSpan())` in
`gatherSpringContextData`, from the map key rather than from anything in the payload.

Adapters therefore never need an `InputFile`, and are stateless singletons
(`BeanDefinitionHolderTypeAdapter`, `TextSpanTypeAdapter`).

### Sharing an entry between rules

Two checks may return the same `cacheKeyPrefix()` to share one entry. `MissingPackageInfoCheck` (S1228) and `UselessPackageInfoCheck` (S4032) already share a single entry this way, through the hand-rolled key of their `AbstractPackageInfoChecker` base class.
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
| `SpringBeansShouldBeAccessibleCheck` (S4605) | Aggregates packages and reports in `endOfAnalysis`, but predates `FileCachingCheck` and still hand-rolls its own plumbing — not a template |
| `BrainMethodCheck` (S6541) | No caching: collects candidates, noise-filters and reports in `endOfAnalysis` |
| `AbstractPackageInfoChecker` (S1228, S4032) | Two rules sharing one cache entry |
| `ExcessiveContentRequestCheck` (S5693) | Cross-file config aggregation |
| `DateEnumsCheck` (S8694) | Caches potential issues and rebuilds their quick fixes without the AST |
| `BeanDefinitionGatherer` | `FileCachingCheck` reference: nested payload, spans paired with their file at aggregation time |
| `ComponentScanPackageGatherer` | `FileCachingCheck` reference: flat payload, no locations to restore |

## Memory warning

> "keeping state between files can lead to memory leaks. Implement with care." — `EndOfAnalysis` javadoc

- Store serializable/primitive data only — never hold AST node references between files
- Clear per-file state in `leaveFile()` (or `setContext()`)
