---
title: Implementing a custom Java rule
description: Use syntax trees and the semantic API to implement the rule and make its unit test pass.
generated:
  by: tirion
---

# Implementing the rule

Before you start, [write the rule specification and failing test](writing-a-rule.md).

## First version: Using syntax trees and API basics

Before running any rule, the SonarQube Java Analyzer parses the Java file into a **syntax tree**. Each Java language construct has a tree `Kind` and an interface describing it. For example, a method declaration has kind `org.sonar.plugins.java.api.tree.Tree.Kind.METHOD` and implements `org.sonar.plugins.java.api.tree.MethodTree`. All kinds are listed in the [`Kind` enum of the Java Analyzer API](https://github.com/SonarSource/sonar-java/blob/7.16.0.30901/java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Tree.java#L47).

`IssuableSubscriptionVisitor` lets a rule subscribe to specific node kinds in `nodesToVisit()` and report issues on the nodes it visits. Our initial implementation returned an empty list, so it received no nodes. In `MyFirstCustomCheck`, replace `nodesToVisit()` to subscribe to method declarations:

```java
@Override
public List<Kind> nodesToVisit() {
  return Collections.singletonList(Kind.METHOD);
}
```

Override `visitNode(Tree tree)` to decide what to do for each method declaration. Import `org.sonar.plugins.java.api.tree.Tree`:

```java
@Override
public void visitNode(Tree tree) {
}
```

Because the rule subscribes only to `Kind.METHOD`, it can safely cast each node to a `MethodTree`. Import `org.sonar.plugins.java.api.tree.MethodTree`. If you subscribe to multiple kinds, use `Tree.is(Kind... kind)` to check the node kind before casting.

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
}
```

First, check that the method has a single parameter and report an issue if it does:

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    reportIssue(method.simpleName(), "Never do that!");
  }
}
```

The `reportIssue(Tree tree, String message)` method reports an issue with the specified message at the location of the given tree. Passing `method.simpleName()` puts the issue precisely on the method name.

Run `MyFirstCustomCheckTest.test()` again. The verifier reports three extra issues:

```text
java.lang.AssertionError: Unexpected at [5, 7, 11]
```

In the [sample file](writing-a-rule.md#a-test-file-to-rule-them-all), line 5 returns `void`, line 7 returns `Object` instead of `int`, and line 11 has a variable-arity parameter, whose type is `int[]`, not `int`. The current implementation treats all one-parameter methods alike. To distinguish these cases, the rule needs type information from the semantic API.

> **When to use `BaseTreeVisitor`**
>
> `IssuableSubscriptionVisitor` is convenient when a rule only needs to visit a few kinds of nodes. If you need more control over traversing the tree, `org.sonar.plugins.java.api.tree.BaseTreeVisitor` has a dedicated visit method for each kind of node.
>
> In the [rules already implemented in the Java Plugin](https://github.com/SonarSource/sonar-java/tree/7.16.0.30901/java-checks/src/main/java/org/sonar/java/checks), some checks use an `IssuableSubscriptionVisitor` as the entry point and a `BaseTreeVisitor` to find a pattern elsewhere in the code.

## Second version: Using semantic API

In addition to the syntax tree, the analyzer constructs a **semantic model**. It represents declarations and references as **symbols**. A method symbol, for example, can tell you where the method is declared and used, its parameter and return types, and which exceptions it can throw. Explore the [semantic package of the API](https://github.com/SonarSource/sonar-java/tree/7.16.0.30901/java-frontend/src/main/java/org/sonar/plugins/java/api/semantic) for more examples.

Once you know the method has a single parameter, get its symbol with `MethodTree.symbol()`. Import `org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol`:

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    MethodSymbol symbol = method.symbol();
    reportIssue(method.simpleName(), "Never do that!");
  }
}
```

The symbol gives you both the **type of its first parameter** and the **return type**. Import `org.sonar.plugins.java.api.semantic.Type`:

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    MethodSymbol symbol = method.symbol();
    Type firstParameterType = symbol.parameterTypes().get(0);
    Type returnType = symbol.returnType().type();
    reportIssue(method.simpleName(), "Never do that!");
  }
}
```

Use `Type.is(String fullyQualifiedName)` to report an issue only if the parameter type and return type match:

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    MethodSymbol symbol = method.symbol();
    Type firstParameterType = symbol.parameterTypes().get(0);
    Type returnType = symbol.returnType().type();
    if (returnType.is(firstParameterType.fullyQualifiedName())) {
      reportIssue(method.simpleName(), "Never do that!");
    }
  }
}
```

Run the test again. It should now pass with issues on `foo3` and `foo5` only.

[*Congratulations! You implemented your first custom rule for the SonarQube Java Analyzer!*](../resources/success.jpg)

## What you can use, and what you can't

Use only the public [SonarJava API](https://github.com/SonarSource/sonar-java/tree/7.16.0.30901/java-frontend/src/main/java/org/sonar/plugins/java/api) when calling into the Java Analyzer from a custom rule. Existing rules use internal utilities that are not available to custom rule plugins at runtime. Your unit tests may pass if those utilities are on the test classpath, but your plugin can fail during analysis.

If you need an API capability that isn't available, suggest it on the [SonarSource community forum](https://community.sonarsource.com/).

Next: [Register the rule in the custom plugin](registering-the-rule.md) · [Tutorial index](index.md)
