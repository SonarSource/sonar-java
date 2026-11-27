---
title: Writing a custom Java rule
description: Specify your first custom Java rule and write a unit test that fails before the rule is implemented.
generated:
  by: tirion
---

# Writing a rule

We will write a custom rule from scratch using [test-driven development](https://en.wikipedia.org/wiki/Test-driven_development) (TDD). Start by writing test cases, then implement the rule against those cases.

## Three files to forge a rule

A rule needs at least three files:

1. A sample file, which contains Java code to analyze in the test.
2. A test class, which runs the rule against the sample file and verifies the issues it reports.
3. A rule class, which implements the rule.

Create these three files in the [example project](getting-started.md):

1. In `docs/java-custom-rules-example/src/test/files`, create **MyFirstCustomCheck.java**:

   ```java
   class MyClass {
   }
   ```

2. In `docs/java-custom-rules-example/src/test/java/org/sonar/samples/java/checks`, create **MyFirstCustomCheckTest.java**:

   ```java
   package org.sonar.samples.java.checks;

   import org.junit.jupiter.api.Test;

   class MyFirstCustomCheckTest {

     @Test
     void test() {
     }

   }
   ```

3. In `docs/java-custom-rules-example/src/main/java/org/sonar/samples/java/checks`, create **MyFirstCustomCheck.java**. Extend `IssuableSubscriptionVisitor` and leave `nodesToVisit()` empty for now:

   ```java
   package org.sonar.samples.java.checks;

   import java.util.Collections;
   import java.util.List;
   import org.sonar.check.Rule;
   import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
   import org.sonar.plugins.java.api.tree.Tree.Kind;

   @Rule(key = "MyFirstCustomRule")
   public class MyFirstCustomCheck extends IssuableSubscriptionVisitor {

     @Override
     public List<Kind> nodesToVisit() {
       return Collections.emptyList();
     }

   }
   ```

> Rules that take parameters or depend on the Java source version may need multiple sample files. You can also add external metadata files such as an HTML description when you [register the rule](registering-the-rule.md#rule-metadata).

## A specification to make it right

Before writing any more code, we need a specification. For this exercise, we'll borrow one from a famous wizard:

> **Gandalf — Why Program When Magic Rulez (WPWMR, p.42)**
>
> *“For a method having a single parameter, the types of its return value and its parameter should never be the same.”*

## A test file to rule them all

Start with examples the rule will encounter, and use a trailing `// Noncompliant` comment on each line where the rule should raise an issue. The sample file should compile. Cover the cases that affect the result, not irrelevant details such as the method name or body, or whether the method belongs to a concrete class, an abstract class, or an interface.

Replace the contents of **MyFirstCustomCheck.java** **in the sample folder** with:

```java
class MyClass {
  MyClass(MyClass mc) { }

  int     foo1() { return 0; }
  void    foo2(int value) { }
  int     foo3(int value) { return 0; } // Noncompliant
  Object  foo4(int value) { return null; }
  MyClass foo5(MyClass value) { return null; } // Noncompliant

  int     foo6(int value, String name) { return 0; }
  int     foo7(int ... values) { return 0; }
}
```

The test file now contains the following cases:

- **Line 2:** A constructor, which the rule should distinguish from a method.
- **Line 4:** A method without parameters (`foo1`).
- **Line 5:** A method returning `void` (`foo2`).
- **Line 6:** A method returning the same primitive type as its parameter (`foo3`), which is noncompliant.
- **Line 7:** A method with a single parameter but a different return type (`foo4`).
- **Line 8:** Another noncompliant method, this time with the same reference type for both its parameter and return type (`foo5`).
- **Line 10:** A method with more than one parameter (`foo6`).
- **Line 11:** A method with a variable-arity parameter (`foo7`).

## A test class to make it pass

In `MyFirstCustomCheckTest`, import `org.sonar.java.checks.verifier.CheckVerifier` and use it to run your check against the sample. The path passed to `onFile()` is relative to the example project directory.

```java
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/MyFirstCustomCheck.java")
      .withCheck(new MyFirstCustomCheck())
      .verifyIssues();
  }
```

`CheckVerifier` initializes the analyzer and checks that the rule reports issues on the lines marked `Noncompliant`, and nowhere else.

Run the test with JUnit. Because `nodesToVisit()` is still empty, the check cannot raise any issues, so the test should fail:

```text
java.lang.AssertionError: No issue raised. At least one issue expected
```

Next: [Implement the rule using syntax trees and the semantic API](implementing-the-rule.md) · [Tutorial index](index.md)
