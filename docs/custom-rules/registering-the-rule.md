---
title: Registering a custom Java rule
description: Add rule metadata, register the check in the custom plugin, and update the registration test.
generated:
  by: tirion
---

# Registering the rule in the custom plugin

Once your [rule passes its unit test](implementing-the-rule.md), register it so SonarQube can list and run it.

## Rule Metadata

Annotations can describe a rule, while separate metadata files let you provide richer descriptions. The template combines the two: `@Rule` supplies the rule key, and `MyJavaRulesDefinition` uses that key to load an HTML description and JSON metadata.

In `docs/java-custom-rules-example/src/main/resources/org/sonar/l10n/java/rules/java`, create **MyFirstCustomRule.html** and **MyFirstCustomRule.json**. Use the rule key, not the Java class name, for both filenames.

In the HTML file, explain the rule and show developers how to fix the issue:

```html
<p>For a method having a single parameter, the types of its return value and its parameter should never be the same.</p>

<h2>Noncompliant Code Example</h2>
<pre>
class MyClass {
  int doSomething(int a) { // Noncompliant
    return 42;
  }
}
</pre>

<h2>Compliant Solution</h2>
<pre>
class MyClass {
  int doSomething() { // Compliant
    return 42;
  }
  long doSomething(int a) { // Compliant
    return 42L;
  }
}
</pre>
```

In **MyFirstCustomRule.json**, set the title, type, status, tags, and default severity:

```json
{
  "title": "Return type and parameter of a method should not be the same",
  "type": "Bug",
  "status": "ready",
  "tags": [
    "bugs",
    "gandalf",
    "magic"
  ],
  "defaultSeverity": "Critical"
}
```

## Rule Activation

In [`RulesList`](../java-custom-rules-example/src/main/java/org/sonar/samples/java/RulesList.java), list the check among the plugin's Java checks. Checks in `getJavaChecks()` run on main sources, while checks in `getJavaTestChecks()` run on test sources. Add the new check as the last entry in `getJavaChecks()`:

```java
public static List<Class<? extends JavaCheck>> getJavaChecks() {
  return Collections.unmodifiableList(Arrays.asList(
    // Keep the other checks here.
    MyFirstCustomCheck.class));
}
```

Import `org.sonar.samples.java.checks.MyFirstCustomCheck` in `RulesList`. [Activation in a quality profile](testing-a-custom-plugin.md) happens after deployment.

## Rule Registrar

The Java Analyzer needs to know which checks your plugin provides. The template already supplies [`MyJavaFileCheckRegistrar`](../java-custom-rules-example/src/main/java/org/sonar/samples/java/MyJavaFileCheckRegistrar.java) and adds it as an extension of [`MyJavaRulesPlugin`](../java-custom-rules-example/src/main/java/org/sonar/samples/java/MyJavaRulesPlugin.java). The registrar maps the check lists to the rule repository:

```java
@SonarLintSide
public class MyJavaFileCheckRegistrar implements CheckRegistrar {

  @Override
  public void register(RegistrarContext registrarContext) {
    registrarContext.registerClassesForRepository(MyJavaRulesDefinition.REPOSITORY_KEY, checkClasses(), testCheckClasses());
  }

  public static List<Class<? extends JavaCheck>> checkClasses() {
    return RulesList.getJavaChecks();
  }

  public static List<Class<? extends JavaCheck>> testCheckClasses() {
    return RulesList.getJavaTestChecks();
  }

}
```

In [`MyJavaFileCheckRegistrarTest`](../java-custom-rules-example/src/test/java/org/sonar/samples/java/MyJavaFileCheckRegistrarTest.java), keep the existing expectations and append the new rule key and class name to the main-check assertions:

```java
class MyJavaFileCheckRegistrarTest {

  @Test
  void checkRegisteredRulesKeysAndClasses() {
    TestCheckRegistrarContext context = new TestCheckRegistrarContext();

    MyJavaFileCheckRegistrar registrar = new MyJavaFileCheckRegistrar();
    registrar.register(context);

    assertThat(context.mainRuleKeys).extracting(RuleKey::toString).containsExactly(
      // Keep the other rule keys here.
      "mycompany-java:MyFirstCustomRule");

    assertThat(context.mainCheckClasses).extracting(Class::getSimpleName).containsExactly(
      // Keep the other class names here.
      "MyFirstCustomCheck");

    assertThat(context.testRuleKeys).extracting(RuleKey::toString).containsExactly(
      "mycompany-java:NoIfStatementInTests");

    assertThat(context.testCheckClasses).extracting(Class::getSimpleName).containsExactly(
      "NoIfStatementInTestsRule");
  }

}
```

## Rules repository

Optionally, customize the repository key and display name in [`MyJavaRulesDefinition`](../java-custom-rules-example/src/main/java/org/sonar/samples/java/MyJavaRulesDefinition.java). If you change the repository key, update the repository prefixes in `MyJavaFileCheckRegistrarTest` as well.

```java
public class MyJavaRulesDefinition implements RulesDefinition {
  // ...
  public static final String REPOSITORY_KEY = "fellowship-inc";

  public static final String REPOSITORY_NAME = "The Fellowship's custom rules";
  // ...
}
```

Next: [Test the custom plugin in SonarQube](testing-a-custom-plugin.md) · [Tutorial index](index.md)
