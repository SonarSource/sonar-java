Writing Custom Java Rules 101
==========

You are using SonarQube and its Java Analyzer to analyze your projects, but there aren't rules that allow you to target some of your company's specific needs? Then your logical choice may be to implement your own set of custom Java rules.

This document is an introduction to custom rule writing for the SonarQube Java Analyzer. It will cover all the main concepts of static analysis required to understand and develop effective rules, relying on the API provided by the SonarSource Analyzer for Java.

## Content

* [Getting Started](#getting-started)
  * [Looking at the pom](#looking-at-the-pom)
* [Writing a rule](#writing-a-rule)
  * [Three files to forge a rule](#three-files-to-forge-a-rule)
  * [A specification to make it right](#a-specification-to-make-it-right)
  * [A test file to rule them all](#a-test-file-to-rule-them-all)
  * [A test class to make it pass](#a-test-class-to-make-it-pass)
  * [First version: Using syntax trees and API basics](#first-version-using-syntax-trees-and-api-basics)
  * [Second version: Using semantic API](#second-version-using-semantic-api)
  * [What you can use, and what you can't](#what-you-can-use-and-what-you-cant)
* [Registering the rule in the custom plugin](#registering-the-rule-in-the-custom-plugin)
  * [Rule Metadata](#rule-metadata)
  * [Rule Activation](#rule-activation)
  * [Rule Registrar](#rule-registrar)
* [Testing a custom plugin](#testing-a-custom-plugin)
  * [How to define rule parameters](#how-to-define-rule-parameters)
  * [How to test sources requiring external binaries](#how-to-test-sources-requiring-external-binaries)
  * [How to test precise issue location](#how-to-test-precise-issue-location)
  * [How to test the Source Version in a rule](#how-to-test-the-source-version-in-a-rule)
* [References](#references)

## Getting started

The rules you are going to develop will be delivered using a dedicated, custom plugin, relying on the **SonarSource Analyzer for Java API**. In order to start working efficiently, we provide a template maven project, that you will fill in while following this tutorial.

Grab the template project by cloning this repository (https://github.com/SonarSource/sonar-java) and then importing in your IDE the sub-module [java-custom-rules-examples](https://github.com/SonarSource/sonar-java/tree/master/docs/java-custom-rules-example).
This project already contains examples of custom rules. Our goal will be to add an extra rule!

### Looking at the POM

A custom plugin is a Maven project, and before diving into code, it is important to notice a few relevant lines related to the configuration of your soon-to-be-released custom plugin. The root of a Maven project is a file named `pom.xml`.

In our case, we have 3 of them:
* `pom.xml`: use a snapshot version of the Java Analyzer
* `pom_SQ_7_9_LTS.xml`: self-contained `pom` file, configured with dependencies matching SonarQube `7.9 LTS` requirements
* `pom_SQ_8_9_LTS.xml`: self-contained `pom` file, configured with dependencies matching SonarQube `8.9 LTS` requirements

These 3 `pom`s correspond different use-cases, depending on which instance of SonarQube you will target with your custom-rules plugin. In this tutorial, **we will only use the file named `pom_SQ_8_9_LTS.xml`**, as it is completely independent from the build of the Java Analyzer, is self-contained, and will target the latest release of SonarQube.

Let's start by building the custom-plugin template by using the following command:

```
mvn clean install -f pom_SQ_8_9_LTS.xml
```

Note that you can also decide to **delete** the original pom.xml file (**NOT RECOMMENDED**), and then rename `pom_SQ_8_9_LTS.xml` into `pom.xml`. You would then be able to use the very simple command:

```
mvn clean install
```

Looking inside the `pom`, you will see that both versions of SonarQube and the Java Analyzer are hard-coded. This is because SonarSource's analyzers are directly embedded in the various SonarQube versions and are shipped together. For instance, SonarQube `7.9` (previous LTS) is shipped with the version `6.3.2.22818` of the Java Analyzer, while SonarQube `8.9` (LTS) is shipped with a much more recent version `6.15.1.26025` of the Java Analyzer. **These versions can not be changed**.

```xml
<properties>
  <sonarqube.version>8.9.0.43852</sonarqube.version>
  <sonarjava.version>6.15.1.26025</sonarjava.version>
  <!-- [...] -->
</properties>
```

Other tags such as `<groupId>`, `<artifactId>`, `<version>`, `<name>` and `<description>` can be freely modified.

```xml
  <groupId>org.sonar.samples</groupId>
  <artifactId>java-custom-rules-example</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <name>SonarQube Java :: Documentation :: Custom Rules Example</name>
  <description>Java Custom Rules Example for SonarQube</description>
```

In the code snippet below, it is important to note that the **entry point of the plugin** is provided as the `<pluginClass>` in the configuration of the sonar-packaging-maven plugin, using the fully qualified name of the java class `MyJavaRulesPlugin`.
If you refactor your code, rename, or move the class extending `org.sonar.api.SonarPlugin`, you will have to change this configuration.
It's also the property `<sonarQubeMinVersion>` which guarantees the compatibility with the SonarQube instance you target.

```xml
<plugin>
  <groupId>org.sonarsource.sonar-packaging-maven-plugin</groupId>
  <artifactId>sonar-packaging-maven-plugin</artifactId>
  <version>1.20.0.405</version>
  <extensions>true</extensions>
  <configuration>
    <pluginKey>java-custom</pluginKey>
    <pluginName>Java Custom Rules</pluginName>
    <pluginClass>org.sonar.samples.java.MyJavaRulesPlugin</pluginClass>
    <sonarLintSupported>true</sonarLintSupported>
    <sonarQubeMinVersion>${sonarqube.version}</sonarQubeMinVersion>
    <requirePlugins>java:${sonarjava.version}</requirePlugins>
  </configuration>
</plugin>
```

## Writing a rule

In this section we will write a custom rule from scratch. To do so, we will use a [Test Driven Development](https://en.wikipedia.org/wiki/Test-driven_development) (TDD) approach, relying on writing some test cases first, followed by the implementation a solution.

### Three files to forge a rule

When implementing a rule, there is always a minimum of 3 distinct files to create:
  1. A test file, which contains Java code used as input data for testing the rule
  1. A test class, which contains the rule's unit test
  1. A rule class, which contains the implementation of the rule.
  
To create our first custom rule (usually called a "*check*"), let's start by creating these 3 files in the template project, as described below:

  1. In folder `/src/test/files`, create a new empty file named `MyFirstCustomCheck.java`, and copy-paste the content of the following code snippet.
```java
class MyClass {
}
```

  2. In package `org.sonar.samples.java.checks` of `/src/test/java`, create a new test class called `MyFirstCustomCheckTest` and copy-paste the content of the following code snippet.
```java
package org.sonar.samples.java.checks;
 
import org.junit.jupiter.api.Test;

class MyFirstCustomCheckTest {

  @Test
  void test() {
  }

}
```

  3. In package `org.sonar.samples.java.checks` of `/src/main/java`, create a new class called `MyFirstCustomCheck` extending class `org.sonar.plugins.java.api.IssuableSubscriptionVisitor` provided by the Java Plugin API. Then, replace the content of the `nodesToVisit()` method with the content from the following code snippet. This file will be described when dealing with implementation of the rule!
```java
package org.sonar.samples.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import java.util.Collections;
import java.util.List;

@Rule(key = "MyFirstCustomRule")
public class MyFirstCustomCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.emptyList();
  }
}

```

>
> :question: **More files...**
> 
> If the 3 files described above are always the base of rule writing, there are situations where extra files may be needed. For instance, when a rule uses parameters, or if its behavior relies on the detected version of java, multiple test files could be required. It is also possible to use external files to describe rule metadata, such as a description in HTML format. Such situations will be described in other topics of this documentation.
>

### A specification to make it right

Of course, before going any further, we need a key element in rule writing: a specification! For the sake of the exercise, lets consider the following quote from a famous Guru as being the specification of our custom rule, as it is of course absolutely correct and incontrovertible.

>
> **Gandalf - Why Program When Magic Rulez (WPWMR, p.42)**
>
> *“For a method having a single parameter, the types of its return value and its parameter should never be the same.”*
>

### A test file to rule them all

Because we chose a TDD approach, the first thing to do is to write examples of the code our rule will target. In this file, we consider numerous cases that our rule may encounter during an analysis, and flag the lines which will require our implementation to raise issues. The flag to be used is a simple `// Noncompliant` trailing comment on the line of code where an issue should be raised. Why *Noncompliant*? Because the flagged lines do not *comply* with the rule.

Covering all the possible cases is not necessarily required, the goal of this file is to cover all the situations which may be encountered during an analysis, but also to abstract irrelevant details. For instance, in the context of our first rule, the name of method, the content of its body, and the owner of the method make no difference, whether it's an abstract class, a concrete class, or an interface. Note that this sample file should be structurally correct and all code should compile.

In the test file `MyFirstCustomCheck.java` created earlier, copy-paste the following code: 
```java
class MyClass {
  MyClass(MyClass mc) { }

  int     foo1() { return 0; }
  void    foo2(int value) { }
  int     foo3(int value) { return 0; } // Noncompliant
  Object  foo4(int value) { return null; }
  MyClass foo5(MyClass value) {return null; } // Noncompliant

  int     foo6(int value, String name) { return 0; }
  int     foo7(int ... values) { return 0;}
}
```

The test file now contains the following test cases:
* **line 2:** A constructor, to differentiate the case from a method;
* **line 4:** A method without parameter (`foo1`);
* **line 5:** A method returning void (`foo2`);
* **line 6:** A method returning the same type as its parameter (`foo3`), which will be noncompliant;
* **line 7:** A method with a single parameter, but a different return type (`foo4`);
* **line 8:** Another method with a single parameter and same return type, but with non-primitive types (`foo5`), therefore non compliant too;
* **line 10:** A method with more than 1 parameter (`foo6`);
* **line 11:** A method with a variable arity argument (`foo7`);

### A test class to make it pass

Once the test file is updated, let's update our test class to use it, and link the test to our (not yet implemented) rule. To do so, get back to our test class `MyFirstCustomCheckTest`, and update the `test()` method as shown in the following code snippet (you may have to import class `org.sonar.java.checks.verifier.CheckVerifier`):

```java
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/MyFirstCustomCheck.java")
      .withCheck(new MyFirstCustomCheck())
      .verifyIssues();
  }
```

As you probably noticed, this test class contains a single test, the purpose of which is to verify the behavior of the rule we are going to implement. To do so, it relies on usage of the `CheckVerifier` class, provided by the Java Analyzer rule-testing API. This `CheckVerifier` class provides useful methods to validate rule implementations, allowing us to totally abstract all the mechanisms related to analyzer initialization. Note that while verifying a rule, the *verifier* will collect lines marked as being *Noncompliant*, and verify that the rule raises the expected issues and *only* those issues.

Now, let's proceed to the next step of TDD: make the test fail!

To do so, simply execute the test from the test file using JUnit. The test should **fail** with error message "**At least one issue expected**", as shown in the code snippet below. Since our check is not yet implemented, no issue can be raised yet, so that's the expected behavior.

```
java.lang.AssertionError: No issue raised. At least one issue expected
    at org.sonar.java.checks.verifier.InternalCheckVerifier.assertMultipleIssues(InternalCheckVerifier.java:291)
    at org.sonar.java.checks.verifier.InternalCheckVerifier.checkIssues(InternalCheckVerifier.java:231)
    at org.sonar.java.checks.verifier.InternalCheckVerifier.verifyAll(InternalCheckVerifier.java:222)
    at org.sonar.java.checks.verifier.InternalCheckVerifier.verifyIssues(InternalCheckVerifier.java:167)
    at org.sonar.samples.java.checks.MyFirstCustomCheckTest.test(MyFirstCustomCheckTest.java:13)
    ...
```

### First version: Using syntax trees and API basics

Before we start with the implementation of the rule itself, you need a little background.

Prior to running any rule, the SonarQube Java Analyzer parses a given Java code file and produces an equivalent data structure: the **Syntax Tree**. Each construction of the Java language can be represented with a specific kind of Syntax Tree, detailing each of its particularities. Each of these constructions is associated with a specific Kind as well as an interface explicitly describing all its particularities. For instance, the kind associated to the declaration of a method will be `org.sonar.plugins.java.api.tree.Tree.Kind.METHOD`, and its interface defined by  `org.sonar.plugins.java.api.tree.MethodTree`. All the kinds are listed in the the [`Kind` enum of the Java Analyzer API](https://github.com/SonarSource/sonar-java/blob/6.13.0.25138/java-frontend/src/main/java/org/sonar/plugins/java/api/tree/Tree.java#L47).

When creating the rule class, we chose to implement the `IssuableSubscriptionVisitor` class from the API. This class, on top of providing a bunch of useful methods to raise issues, also **defines the strategy which will be used when analyzing a file**. As its name is telling us, it is based on a subscription mechanism, allowing to specify on what kind of tree the rule should react. The list of node types to cover is specified through the `nodesToVisit()` method. In the previous steps, we modified the implementation of the method to return an empty list, therefore not subscribing to any node of the syntax tree.

Now it's finally time to jump in to the implementation of our first rule! Go back to the `MyFirstCustomCheck` class, and modify the list of Kinds returned by the nodesToVisit() method. Since our rule targets method declarations, we only need to visit methods. To do so, simply make sure that we return a singleton list containing only `Kind.METHOD` as a parameter of the returned list, as shown in the following code snippet.

```java
@Override
public List<Kind> nodesToVisit() {
  return Collections.singletonList(Kind.METHOD);
}
```

Once the nodes to visit are specified, we have to implement how the rule will react when encountering method declarations. To do so, override method `visitNode(Tree tree)`, inherited from `SubscriptionVisitor` through `IssuableSubscriptionVisitor`.

```java
@Override
public void visitNode(Tree tree) {
}
```

Because we registered the rule to visit Method nodes, we know that every time the method is called, the tree parameter will be a `org.sonar.plugins.java.api.tree.MethodTree` (the interface tree associated with the `METHOD` kind). As a first step, we can consequently safely cast the tree directly into a MethodTree, as shown below. Note that if we had registered multiple node types, we would have to test the node kind before casting by using the method `Tree.is(Kind ... kind)`.

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
}
```

Now, let's narrow the focus of the rule by checking that the method has a single parameter, and raise an issue if it's the case.

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    reportIssue(method.simpleName(), "Never do that!");
  }
}
```

The method `reportIssue(Tree tree, String message)` from `IssuableSubscriptionVisitor` allows to report an issue on a given tree with a specific message. In this case, we chose to report the issue at a precise location, which will be the name of the method.

Now, let's test our implementation by executing `MyFirstCustomCheckTest.test()` again.

```
java.lang.AssertionError: Unexpected at [5, 7, 11]
    at org.sonar.java.checks.verifier.InternalCheckVerifier.assertMultipleIssues(InternalCheckVerifier.java:303)
    at org.sonar.java.checks.verifier.InternalCheckVerifier.checkIssues(InternalCheckVerifier.java:231)
    at org.sonar.java.checks.verifier.InternalCheckVerifier.verifyAll(InternalCheckVerifier.java:222)
    at org.sonar.java.checks.verifier.InternalCheckVerifier.verifyIssues(InternalCheckVerifier.java:167)
    at org.sonar.samples.java.checks.MyFirstCustomCheckTest.test(MyFirstCustomCheckTest.java:13)
    ...
```

Of course, our test failed again... The `CheckVerifier` reported that lines 5, 7 and 11 are raising unexpected issues, as visible in the stack-trace above. By looking back at our test file, it's easy to figure out that raising an issue line 5 is wrong because the return type of the method is `void`, line 7 is wrong because `Object` is not the same as `int`, and line 11 is also wrong because of the variable *arity* of the method. Raising these issues is however correct accordingly to our implementation, as we didn't check for the types of the parameter and return type. To handle type, however, we will need to rely on more that what we can achieve using only knowledge of the syntax tree. This time, we will need to use the semantic API!

>
> :question: **IssuableSubscriptionVisitor and BaseTreeVisitor**
>
> For the implementation of this rule, we chose to use an `IssuableSubscriptionVisitor` as the implementation basis of our rule. This visitor offers an easy approach to writing quick and simple rules, because it allows us to narrow the focus of our rule to a given set of Kinds to visit by subscribing to them. However, this approach is not always the most optimal one. In such a situation, it could be useful to take a look at another visitor provided with the API: `org.sonar.plugins.java.api.tree.BaseTreeVisitor`. The `BaseTreeVisitor` contains a `visit()` method dedicated for each and every kind of the syntax tree, and is particularly useful when the visit of a file has to be fine tuned.
>
> In [rules already implemented in the Java Plugin](https://github.com/SonarSource/sonar-java/tree/5.12.1.17771/java-checks/src/main/java/org/sonar/java/checks), you will be able to find multiple rule using both approaches: An `IssuableSubscriptionVisitor` as entry point, helped by simple `BaseTreeVisitor`(s) to identify pattern in other parts of code.
>

### Second version: Using semantic API

Up to now, our rule implementation only relied on the data provided directly by syntax tree that resulted from the parsing of the code. However, the SonarAnalyzer for Java provides a lot more regarding the code being analyzed, because it also constructs a ***semantic model*** of the code. This semantic model provides information related to each ***symbol*** being manipulated. For a method, for instance, the semantic API will provide useful data such as a method's owner, its usages, the types of its parameters and its return type, the exception it may throw, etc. Don't hesitate to explore the [semantic package of the API](https://github.com/SonarSource/sonar-java/tree/6.13.0.25138/java-frontend/src/main/java/org/sonar/plugins/java/api/semantic) in order to have an idea of what kind of information you will have access to during analysis!

But now, let's go back to our implementation and take advantage of the semantic.

Once we know that our method has a single parameter, let's start by getting the symbol of the method using the `symbol()` method from the `MethodTree`.

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

From the symbol, it is then pretty easy to retrieve **the type of its first parameter**, as well as the **return type** (You may have to import `org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol` and `org.sonar.plugins.java.api.semantic.Type`).

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    Symbol.MethodSymbol symbol = method.symbol();
    Type firstParameterType = symbol.parameterTypes().get(0);
    Type returnType = symbol.returnType().type();
    reportIssue(method.simpleName(), "Never do that!");
  }
}
```

Since the rule should only raise an issue when these two types are the same, we then simply test if the return type is the same as the type of the first parameter using method `is(String fullyQualifiedName)`, provided through the `Type` class, before raising the issue.

```java
@Override
public void visitNode(Tree tree) {
  MethodTree method = (MethodTree) tree;
  if (method.parameters().size() == 1) {
    Symbol.MethodSymbol symbol = method.symbol();
    Type firstParameterType = symbol.parameterTypes().get(0);
    Type returnType = symbol.returnType().type();
    if (returnType.is(firstParameterType.fullyQualifiedName())) {
      reportIssue(method.simpleName(), "Never do that!");
    }
  }
}
```

Now, **execute the test** class again.

Test passed? If not, then check if you somehow missed a step.

If it passed...

>
> :tada: **Congratulations!** :confetti_ball:
>
> [*You implemented your first custom rule for the SonarQube Java Analyzer!*](resources/success.jpg)
>

### What you can use, and what you can't

When writing custom Java rules, you can only use classes from package [`org.sonar.plugins.java.api`](https://github.com/SonarSource/sonar-java/tree/6.13.0.25138/java-frontend/src/main/java/org/sonar/plugins/java/api).

When browsing the existing 600+ rules from the SonarSource Analyzer for Java, you will sometime notice use of some other utility classes, not part of the API. While these classes could be sometime extremely useful in your context, **these classes are not available at runtime** for custom rule plugins. It means that, while your unit tests are still going to pass when building your plugin, your rules will most likely make analysis **crash at analysis time**.

Note that we are always open to discussion, so don't hesitate to reach us and participate to threads, through our [community forum](https://community.sonarsource.com/), to suggest features and API improvement!

## Registering the rule in the custom plugin

OK, you are probably quite happy at this point, as our first rule is running as expected... However, we are not really done yet. Before playing our rule against any real projects, we have to finalize its creation within the custom plugin, by registering it.

### Rule Metadata
The first thing to do is to provide our rule all the metadata which will allow us to register it properly in the SonarQube platform.
There are 2 ways to add metadata for your rule: annotation and static documentation.
While annotation provides a handy way to document the rule, static documentation offers the possibility for richer information.
Incidentally, static documentation is also the way rules in the sonar-java analyzer are described.

To provide metadata for your rule, you need to create an HTML file, where you can provide an extended textual description of the rule, and a JSON file, with the actual metadata.
In the case of `MyFirstCustomCheck`, you will head to the `src/main/resources/org/sonar/l10n/java/rules/java/` folder to create `MyFirstCustomCheck.html` and `MyFirstCustomCheck.json`.
Please note that both files are needed to register our rule but the HTML one can be left empty.
We can now add metadata to `src/main/resources/org/sonar/l10n/java/rules/java/MyFirstCustomCheck.json`:
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
With this example, we have a concise but descriptive `title` for our rule, the `type` of issue it highlights, its `status` (ready or deprecated), the `tags` that should bring it up in a search and the `severity` of the issue.
Further information can be fed to SonarQube by describing the context in the HTML file or by adding fields in the JSON document but this minimal example should be enough to register our rule.
### Rule Activation
The second thing to do is to activate the rule within the plugin. To do so, open class `RulesList` (`org.sonar.samples.java.RulesList`). In this class, you will notice methods `getJavaChecks()` and `getJavaTestChecks()`. These methods are used to register our rules with alongside the rule of the Java plugin. Note that rules registered in `getJavaChecks()` will only be played against source files, while rules registered in `getJavaTestChecks()` will only be played against test files. To register the rule, simply add the rule class to the list builder, as in the following code snippet:

```java
public static List<Class<? extends JavaCheck>> getJavaChecks() {
  return Collections.unmodifiableList(Arrays.asList(
      // other rules...
      MyFirstCustomCheck.class
    ));
}

```

### Rule Registrar

Because your rules are relying on the SonarSource Analyzer for Java API, you also need to tell the parent Java plugin that some new rules have to be retrieved. If you are using the template custom plugin as a base of this tutorial, you should have everything done already, but feel free to have a look at the `MyJavaFileCheckRegistrar.java` class, which connects the dots. Finally, be sure that this registrar class is also correctly added as an extension for your custom plugin, by adding it to your Plugin definition class (`MyJavaRulesPlugin.java`).

```java
/**
 * Provide the "checks" (implementations of rules) classes that are going be executed during
 * source code analysis.
 *
 * This class is a batch extension by implementing the {@link org.sonar.plugins.java.api.CheckRegistrar} interface.
 */
@SonarLintSide
public class MyJavaFileCheckRegistrar implements CheckRegistrar {
 
  /**
   * Register the classes that will be used to instantiate checks during analysis.
   */
  @Override
  public void register(RegistrarContext registrarContext) {
    // Call to registerClassesForRepository to associate the classes with the correct repository key
    registrarContext.registerClassesForRepository(MyJavaRulesDefinition.REPOSITORY_KEY, checkClasses(), testCheckClasses());
  }
 
 
  /**
   * Lists all the main checks provided by the plugin
   */
  public static List<Class<? extends JavaCheck>> checkClasses() {
    return RulesList.getJavaChecks();
  }
 
  /**
   * Lists all the test checks provided by the plugin
   */
  public static List<Class<? extends JavaCheck>> testCheckClasses() {
    return RulesList.getJavaTestChecks();
  }
 
}
```

Now, because we added a new rule, we also need to update our tests to make sure it is taken into account. To do so, navigate to its corresponding test class, named `MyJavaFileCheckRegistrarTest`, and update the expected number of rules from 8 to 9.

```java

class MyJavaFileCheckRegistrarTest {

  @Test
  void checkNumberRules() {
    CheckRegistrar.RegistrarContext context = new CheckRegistrar.RegistrarContext();

    MyJavaFileCheckRegistrar registrar = new MyJavaFileCheckRegistrar();
    registrar.register(context);

    assertThat(context.checkClasses()).hasSize(8); // change it to 9, we added a new one!
    assertThat(context.testCheckClasses()).isEmpty();
  }
}
```

## Testing a custom plugin

>
> :exclamation: **Prerequisite**
> 
> For this chapter, you will need a local instance of SonarQube. If you don't have a SonarQube platform installed on your machine, now is time to download its latest version from [HERE](https://www.sonarqube.org/downloads/)!
>

At this point, we've completed the implementation of a first custom rule and registered it into the custom plugin. The last remaining step is to test it directly with the SonarQube platform and try to analyze a project! 

Start by building the project using maven. Note that here we are using the self-contained `pom` file targeting SonarQube `8.9` LTS. If you renamed it into `pom.xml`, remove the `-f pom_SQ_8_9_LTS.xml` part of the following command):


```
$ pwd
/home/gandalf/workspace/sonar-java/docs/java-custom-rules-example
  
$ mvn clean install -f pom_SQ_8_9_LTS.xml
[INFO] Scanning for projects...
[INFO]                                                                        
[INFO] ------------------------------------------------------------------------
[INFO] Building SonarQube Java :: Documentation :: Custom Rules Example 1.0.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
  
...
 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 8.762 s
[INFO] Finished at: 2021-03-02T12:17:28+01:00
[INFO] ------------------------------------------------------------------------
```

Then, grab the jar file `java-custom-rules-example-1.0.0-SNAPSHOT.jar` from the `target` folder of the project, and move it to the extensions folder of your SonarQube instance, which will be located at `$SONAR_HOME/extensions/plugins`.

>
> :exclamation: **SonarQube Java Plugin compatible version**
>
> Before going further, be sure to have the adequate version of the SonarQube Java Analyzer with your SonarQube instance. The dependency over the Java Analyzer of our custom plugin is defined in its `pom`, as seen in the first chapter of this tutorial. We consequently provide two distinct `pom` files mapping both the `7.9` previous LTS version of SonarQube, as well as the latest LTS release, version `8.9`.
>
> * If your instance is SonarQube `7.9` version, make sure to update the Java Analyzer to its latest compatible version through the SonarQube marketplace (it should be version `6.3.2.22818`), and then use this the file `pom_SQ_7_9_LTS.xml` file to build the project. 
> * If you are using a SonarQube `8.9` and updated to the latest LTS already, then you won't have the possibility to update the Java Analyzer independently anymore. Consequently, use the file `pom_SQ_8_9_LTS.xml` to build the project.
>

Now, (re-)start your SonarQube instance, log as admin and navigate to the ***Rules*** tab.

From there, under the language section, select "**Java**", and then "**MyCompany Custom Repository**" under the repository section. Your rule should now be visible (with all the other sample rules). 

![Selected rules](resources/rules_selected.png)

Once activated (not sure how? see [quality-profiles](https://docs.sonarqube.org/latest/instance-administration/quality-profiles/)), the only step remaining is to analyze one of your project!

When encountering a method returning the same type as its parameter, the issue will now raise issue, as visible in the following picture:

![Issues](resources/issues.png)

### How to define rule parameters

You have to add a `@RuleProperty` to your Rule.

Check this example: [SecurityAnnotationMandatoryRule.java](https://github.com/SonarSource/sonar-java/blob/master/docs/java-custom-rules-example/src/main/java/org/sonar/samples/java/checks/SecurityAnnotationMandatoryRule.java)

### How to test sources requiring external binaries

In the `pom.xml`, define in the `Maven Dependency Plugin` part all the JARs you need to run your Unit Tests. For example, if you sample code used in your Unit Tests is having a dependency on Spring, add it there.

See: [pom.xml#L137-L197](./java-custom-rules-example/pom_SQ_8_9_LTS.xml#L137-L197)

### How to test precise issue location

You can raise an issue on a given line, but you can also raise it at a specific Token. Because of that, you may want to specify, in your sample code used by your Unit Tests, the exact location, i.e. in between which 2 specific Columns, where you are expecting the issue to be raised.

This can be achieved using the special keywords `sc` (start-column) and `ec` (end-column) in the `// Noncompliant` comment. In the following example, we are expecting to have the issue being raised between the column 27 and 32 (i.e. exactly on "Order" variable type):

```java
public String updateOrder(Order order) { // Noncompliant [[sc=27;ec=32]] {{Don't use Order here because it's an @Entity}}
```

### How to test the Source Version in a rule

Starting from **Java Plugin API 3.7** (October 2015), the java source version can be accessed directly when writing custom rules. This can be achieved by simply calling the method `getJavaVersion()` from the context. Note that the method will return null only when the property is not set. Similarly, it is possible to specify to the verifier a version of Java to be considered as runtime execution, calling method `verify(String filename, JavaFileScanner check, int javaVersion)`.

```java
@Beta
public interface JavaFileScannerContext {

  // ...
  
  @Nullable
  Integer getJavaVersion();
  
}
```

## References

* [Analysis of Java code documentation](https://docs.sonarqube.org/latest/analysis/languages/java/)
* [SonarQube Platform](http://www.sonarqube.org/)
* [SonarSource Code Quality and Security for Java Github Repository](https://github.com/SonarSource/sonar-java)
* [SonarQube Java Custom-Rules Example](https://github.com/SonarSource/sonar-java/tree/master/docs/java-custom-rules-example)
