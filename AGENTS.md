# Introduction

SonarJava is a static code analyzer for Java.

The rules are implemented in **Java** using the SonarJava API. Test resources are **Java** files annotated with special comment markers.

For step-by-step guidance on implementing a new rule (including metadata generation), see `.claude/skills/new-rule/SKILL.md`.

---


# General Coding Guidelines

- Do **NOT** write comments, inside the code. Comments can easily become outdated.
- Do write documentation for complex functions.
- Use KISS (Keep It Simple, Stupid) and DRY (Don't Repeat Yourself) principles.

---

# Repository Structure

## Maven Module Structure

```
sonar-java/
├── java-frontend/              # Parser, AST, semantic model, type inference
├── java-checks/                # Community rule implementations
│   ├── src/main/java/org/sonar/java/checks/     # Rule Java source
│   └── src/test/
│       ├── java/org/sonar/java/checks/           # Test classes
│       └── files/checks/                          # Legacy Java test resource files. Should be migrated to java-checks-test-sources 
├── java-checks-test-sources/   # Shared Java test resource files used across modules
├── java-checks-aws/            # AWS-specific rule implementations
├── java-symbolic-execution/    # Symbolic execution engine
├── sonar-java-plugin/          # Plugin packaging
└── its/                        # Integration tests
    └── ruling/                 # Ruling integration tests
```

## Rule Implementation Pattern

Rules are Java classes annotated with `@Rule(key = "SXXXX")` that extend one of:

- **`IssuableSubscriptionVisitor`** — Event-based: register tree consumers for specific tree kinds
- **`BaseTreeVisitor`** — Visitor pattern: override `visit*` methods; combined with `JavaFileScanner` to report issues
- **`JavaFileScanner`** — Full scan: override `scanFile` to traverse the entire compilation unit

To avoid coupling between rules, never introduce new abstract classes from which rules would inherit.
To avoid duplicating code, you may use utility classes.

### Example: IssuableSubscriptionVisitor

```java
@Rule(key = "S1234")
public class MyCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    // Analyze the invocation...
    reportIssue(mit, "Message describing the issue.");
  }
}
```

### Example: BaseTreeVisitor

```java
@Rule(key = "S1234")
public class MyCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    // Analyze the invocation...
    context.reportIssue(this, tree, "Message describing the issue.");
    super.visitMethodInvocation(tree);
  }
}
```

### Key API Elements

- **`reportIssue(tree, message)`** — Report an issue at a specific AST node
- **`reportIssue(tree, message, secondaries, cost)`** — Report with secondary locations
- **`Tree.Kind.*`** — AST node type enumeration (e.g., `Tree.Kind.METHOD`, `Tree.Kind.IF_STATEMENT`, `Tree.Kind.METHOD_INVOCATION`)
- **`ExpressionUtils.getParentOfType(tree, Tree.Kind.METHOD)`** — Navigate up the AST
- **`ExpressionUtils`**, **`MethodMatchers`** — Utility classes for common analysis patterns

### MethodMatchers

Use `MethodMatchers` to match method calls by type, name, and signature:

```java
private static final MethodMatchers MY_MATCHER = MethodMatchers.create()
  .ofTypes("java.util.List")
  .names("add")
  .withAnyParameters()
  .build();

// In visitNode:
if (MY_MATCHER.matches(methodInvocationTree)) {
  reportIssue(methodInvocationTree, "Message.");
}
```

### Quick Fix API

```java
JavaQuickFix quickFix = JavaQuickFix.newQuickFix("Remove redundant check")
  .addTextEdit(JavaTextEdit.removeTree(tree))
  .build();
reportIssue(tree, "Message").withQuickFix(() -> quickFix);
```

### Dependency-Aware Rules

When a rule should only apply when a specific library is present, or when its behaviour depends on a dependency version, implement `DependencyVersionAware` alongside the visitor base class:

```java
@Rule(key = "S2230")
public class TransactionalMethodVisibilityCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  private boolean isSpring6OrLater = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  /** Called before analysis. Return false to disable the rule entirely when required dependencies are absent. */
  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> springTxVersion = dependencyFinder.apply("spring-tx");
    Optional<Version> springContextVersion = dependencyFinder.apply("spring-context");
    if (springTxVersion.isEmpty() && springContextVersion.isEmpty()) {
      return false; // rule is disabled — dependency not on classpath
    }
    isSpring6OrLater = springContextVersion
      .or(() -> springTxVersion)
      .map(v -> v.isGreaterThanOrEqualTo("6.0"))
      .orElse(false);
    return true;
  }

  @Override
  public void visitNode(Tree tree) {
    // use isSpring6OrLater to adjust rule behaviour
  }
}
```

Key points:
- `dependencyFinder.apply("artifact-id")` looks up a dependency by its Maven artifact ID and returns its version if present.
- Returning `false` from `isCompatibleWithDependencies` disables the rule for the current project — use this when the rule only makes sense if a particular library is on the classpath.
- Store any version information in an instance field (e.g. `isSpring6OrLater`) so that `visitNode` can use it during the scan.

---

# Understanding Rule Trigger Requirements

When creating simple examples or fixing false positives, examine the rule's test files to understand what conditions trigger the rule:

## AST Node Kinds

Rules subscribe to specific tree kinds. Check `nodesToVisit()` to see which `Tree.Kind.*` values the rule listens to:
- `Tree.Kind.METHOD` — Method declarations
- `Tree.Kind.METHOD_INVOCATION` — Method calls
- `Tree.Kind.IF_STATEMENT` — If statements
- `Tree.Kind.FOR_STATEMENT` — For loops
- `Tree.Kind.VARIABLE` — Variable declarations

## Semantic Model

SonarJava provides a semantic model to resolve types and symbols:
- **`symbol.type()`** — Resolved type of symbol
- **`symbol.owner()`** — Enclosing symbol (e.g., class owning a method)
- **`expressionTree.symbolType()`** — Type of expression
- **`type.isSubtypeOf("java.util.Collection")`** — Type hierarchy checks

## Java Version Context

Rules may behave differently depending on the Java language level:
- Some rules target Java 8+ features (lambdas, streams, `Optional`)
- Some handle Java 11+ or Java 17+ features (var, records, sealed classes)

---

# Test File Conventions

The file general structure for a rule implementation and its tests is as follows:
- Rule class: `java-checks/src/main/java/org/sonar/java/checks/{RuleId}Check.java`
- Test class: `java-checks/src/test/java/org/sonar/java/checks/{RuleId}CheckTest.java`
- Test samples: `java-checks-test-sources/default/src/main/java/checks/{RuleId}CheckSample.java`

## Test Class

The test class should use `CheckVerifier` and test the rule both with and without semantic. 
This is to prevent false positives when the rule is applied to code that does not have the required dependencies on the classpath.
Example:

```java
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class AbsOnNegativeCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AbsOnNegativeCheckSample.java"))
      .withCheck(new AbsOnNegativeCheck())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AbsOnNegativeCheckSample.java"))
      .withCheck(new AbsOnNegativeCheck())
      .withoutSemantic()
      .verifyIssues();
  }
}
```

## Test samples
Test resource files use inline comment markers to declare expected issues:

```java
int x = foo(); // Noncompliant
int y = bar(); // Noncompliant {{Expected message text}}
int z = baz(); // compliant
```

- `// Noncompliant` — The analyzer must report an issue on this line
- `// Noncompliant {{message}}` — Issue must have this exact message
- `// compliant` — Optional annotation for clarity; no issue expected

Secondary locations use `[[...]]` inline markers when needed.

The precise locations of the issues are underlined using `^` characters. To produce that underline use a script like:
```shell
str="<line including <substring>>"; sub="<substring>"; pre="${str%%"$sub"*}" suf="${str#*"$sub"}"; echo "${pre//?/ }${sub//?/^}"
```

---

# Ruling Tests

Ruling tests validate that rule fixes don't break existing true positives. They run the analyzer against real-world Java projects (`guava`, `commons-beanutils`, `eclipse-jetty`, `sonar-server`, `jboss-ejb3-tutorial`, `regex-examples`) and compare results to expected baselines.

- Expected ruling files: `its/ruling/src/test/resources/<project>/java-<RULE_ID>.json`
- Actual ruling output: `its/ruling/target/actual/<project>/java-<RULE_ID>.json`
- Format: `{"group:artifact:path/to/File.java": [line1, line2, ...], ...}`
- Ruling sources: `its/sources/<project>/`

Do not try to run these locally. A PR should be created automatically when they fail on CI.

---
