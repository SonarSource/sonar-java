---
title: Writing SonarJava rules
description: Choose a visitor, match Java constructs semantically, and report issues without leaking state between files.
generated:
  by: tirion
---

# Writing SonarJava rules

## Choose a visitor

Use an existing analyzer API directly, not a base class shared by rules. See the [inheritance decision](../adrs/no-abstract-rule-bases.md). Keep implementation comments out of rule code: they can drift from the rule's behavior. Give logic descriptive names, put non-obvious constraints in the docs, and use sample markers to assert the intended behavior.

- `java-frontend/src/main/java/org/sonar/plugins/java/api/IssuableSubscriptionVisitor.java`: use for checks on known `Tree.Kind` values. The analyzer drives the traversal. Subscribe in `nodesToVisit()`, inspect in `visitNode()`, and use `leaveNode()` only when you need work after the children are visited.
- `java-frontend/src/main/java/org/sonar/plugins/java/api/tree/BaseTreeVisitor.java`: use with `JavaFileScanner` when you need to control descent, for example to stop at nested classes. Call `scan(context.getTree())` from `scanFile()` and call the matching `super.visit*` method when the traversal should continue. A local visitor can capture the file context instead of storing it on the check.
- `java-frontend/src/main/java/org/sonar/plugins/java/api/JavaFileScanner.java`: implement directly when the rule needs one callback per file and can inspect the context without a visitor.

Subscribe to the smallest node with enough context to decide. For example, check an invocation in `METHOD_INVOCATION`; subscribe to `METHOD` instead if the decision depends on several statements in its body.

## Match types and calls

Use `java-frontend/src/main/java/org/sonar/plugins/java/api/semantic/MethodMatchers.java` to match a method's owner, name and parameter signature. Use `ofTypes` for exact declaring types, `ofSubTypes` when subtypes also count, and `addParametersMatcher` when only specific overloads are relevant. Reserve `withAnyParameters()` for checks that apply to all overloads. Do not compare raw method or type names from source text; text does not distinguish overloads, unrelated types with the same simple name, or unresolved symbols.

```java
private static final MethodMatchers LIST_CONTAINS = MethodMatchers.create()
  .ofSubTypes("java.util.List")
  .names("contains")
  .addParametersMatcher("java.lang.Object")
  .build();
```

Use `symbolType().is("java.lang.String")` for an exact type and `isSubtypeOf("java.util.Collection")` for a hierarchy match. Use `Symbol` and its owner when identity or declaration matters. Guard unknown types and symbols before relying on a negative result: failure to resolve a type is not evidence that the type is wrong. Test that fallback explicitly; see [Rule tests](rule-tests.md).

## Gate checks on dependency or Java versions

`java-frontend/src/main/java/org/sonar/plugins/java/api/DependencyVersionAware.java` is called when scanners are selected for a module. The lookup takes an artifact ID such as `spring-web` and returns a `Version` if detected on the classpath. A missing result may mean the classpath cannot be identified, not necessarily that the library is absent. If the rule only makes sense for a known dependency or version, return `false` on absence; `false` disables the rule for that module. If the rule still applies but behaves differently by version, assign the version flag in the callback, including the fallback when the lookup is empty.

`java-frontend/src/main/java/org/sonar/plugins/java/api/JavaVersionAwareVisitor.java` gates on the analyzed project's Java version, not the JVM that runs the analyzer. Use it for checks tied to syntax or APIs that require a particular source version.

## Report the smallest useful span

Use `reportIssue(tree, message)` or the start/end tree overload for a source span. Use secondary `JavaFileScannerContext.Location` entries when another location explains the finding, and pass a remediation cost only when the rule needs to compute it. For a file-level issue, use `addIssueOnFile`. Quick fixes use a separate builder; see [Quick-fix tests](quickfix-tests.md).

## Keep scan state local

Follow the [stateless rules decision](../adrs/stateless-rules.md) for new checks. For existing stateful subscription checks, `clearState()` runs before and after the file, even when analysis fails. For existing direct scanners, release per-file collections and AST references in a `finally` block around traversal. Do not clear a cached value derived only from unchanged `@RuleProperty` configuration: it does not contain file data and clearing it would recompute the same value for every file.

If the rule truly depends on other files, see [Project-level rules](project-level-rules.md) before choosing an aggregation and cache strategy.
