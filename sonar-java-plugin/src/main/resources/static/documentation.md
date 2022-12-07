---
title: Java
key: java
---

<!-- static -->
<!-- update_center:java -->
<!-- /static -->


## Language-Specific Properties

You can discover and update the Java-specific [properties](/analysis/analysis-parameters/) in:  <!-- sonarcloud -->Project <!-- /sonarcloud -->[Administration > General Settings > Java](/#sonarqube-admin#/admin/settings?category=java)

## Java Analysis and Bytecode

Compiled `.class` files are required for java projects with more than one java file. If not provided properly, analysis will fail with the message:

    Your project contains .java files, please provide compiled classes with sonar.java.binaries property, or exclude them from the analysis with sonar.exclusions property.

If only some `.class` files are missing, you'll see warnings like this:

    Class 'XXXXXX' is not accessible through the ClassLoader.

If you are **not** using Maven or Gradle for analysis, you must manually provide bytecode to the analysis.
You can also analyze test code, and for that you need to provide tests binaries and test libraries properties.

Key | Value
---|---|
`sonar.java.binaries` (required) | Comma-separated paths to directories containing the compiled bytecode files corresponding to your source files. 
`sonar.java.libraries` | Comma-separated paths to files with third-party libraries (JAR or Zip files) used by your project. Wildcards can be used: `sonar.java.libraries=path/to/Library.jar,directory/**/*.jar`
`sonar.java.test.binaries` | Comma-separated paths to directories containing the compiled bytecode files corresponding to your test files
`sonar.java.test.libraries` | Comma-separated paths to files with third-party libraries (JAR or Zip files) used by your tests. (For example, this should include the junit jar). Wildcards can be used: `sonar.java.test.libraries=directory/**/*.jar`

[[warning]]
| Android users, Jack doesn't provide the required `.class` files.

## Project's specific JDK

In some situations, you might have to analyze a project built with a different version of Java than the one executing the analysis.
The most common case is to run the analysis with **Java 11**, while the project itself uses **Java 8** or before for its build.

If this is your case, you will need to set the `sonar.java.jdkHome` property manually to point the appropriate JDK (see below).
By doing this you will specify which JDK classes the analyzer must refer to during the analysis.
Not setting this property, while it would have been required, usually leads to inconsistent or even impossible to fix issues being reported, especially in relation with native JDK classes.

When setting `sonar.java.jdkHome`, you need to provide the path to the JDK directory used by the project being analyzed, if different from the Java runtime executing the analysis.
For example, for a Java 8 project, by setting it as follows: `sonar.java.jdkHome=/usr/lib/jvm/jdk1.8.0_211`

```bash
# Here maven uses the default version of Java on the system but we specify that we want to analyze a Java 8 project.
mvn clean verify sonar:sonar \
  # other analysis parameters
  -Dsonar.java.jdkHome=/usr/lib/jvm/java-8-openjdk/
  # other analysis parameters
```
This option can of course be added to your `sonar.properties` configuration.


## Turning issues off

The best way to deactivate an individual issue you don't intend to fix is to mark it "Won't Fix" or "False Positive" through the {instance} UI.

If you need to deactivate a rule (or all rules) for an entire file, then [issue exclusions](/project-administration/narrowing-the-focus/) are the way to go. But if you only want to deactivate a rule across a subset of a file - all the lines of a method or a class - you can use `@SuppressWarnings("all")` or `@SuppressWarnings` with rule keys: `@SuppressWarnings("java:S2077")` or `@SuppressWarnings({"java:S1118", "java:S3546"})`.

## Handling Java Source Version

Java analysis is able to react to the java version used for sources. This feature allows the deactivation of rules that target higher versions of Java than the one in use in the project so that false positives aren't generated from irrelevant rules.

The feature relies entirely on the `sonar.java.source` property, which is automatically filled by most of the scanners used for analyses (Maven, Gradle). Java version-specific rules are not disabled when `sonar.java.source` is not provided. Concretely, rules which are designed to target specific java versions (tagged "java7" or "java8") are activated by default in the Sonar Way Java profile. From a user perspective, the feature is fully automatic, but it means that you probably want your projects to be correctly configured.

When using SonarScanner to perform analyses of project, the property `sonar.java.source` can to be set manually in `sonar-project.properties`. Accepted formats are:
* "1.X" (for instance 1.6 for java 6, 1.7 for java 7, 1.8 for java 8, etc.)
* "X" (for instance 7 for java 7, 8 for java 8, etc. )

Example: `sonar.java.source=1.6`

If the property is provided, the analysis will take the source version into account, and execute related rules accordingly. At run time, each of these rules will be executed – or not – depending of the Java version used by sources within the project. For instance, on a correctly configured project built with Java 6, rules targeting Java 7 and Java 8 will never raise issues, even though they are enabled in the associated rule profile.

## Batch mode settings

By default, files are parsed in batches. The size of the batch is dynamically computed based on the maximum memory available.
It is possible to manually set this value by using the property `sonar.java.experimental.batchModeSizeInKB`.
Note that the perfect value depends on the project and the ecosystem setup, bigger batch size will not necessarily increase the performance and can even slow things down if the memory is a limiting factor.
If needed, it is possible to run the parsing file by file by setting `sonar.java.fileByFile=true`.

More details can be found [here](https://github.com/SonarSource/sonar-java/wiki/Batch-mode).

## Skipping unchanged files
Starting from April 2022, and by default, the Java analyzer optimizes the analysis of unchanged files in pull requests.
In practice, this means that on a file that has not changed, the analyzer only runs a restricted list of rules.
To get a better understanding of the rule exclusion mechanism, keep in mind that:
* Rules that need to run on multiple files to decide whether they need to raise issues are always executed
* Rules that need to run at the end of the analysis to decide whether they need to raise issues are always executed
* Rules that are defined outside of the `sonar.java.checks` package are always executed

This last criteria implies that [custom rules](https://redirect.sonarsource.com/doc/java-custom-rules-guide.html) cannot be skipped.

If you wish to disable this optimization, you can set the value of the analysis parameter `sonar.java.skipUnchanged` to `false`.
Leaving the parameter unset lets the server decide whether the optimization should be enabled.

## Cache-enabled rules (experimental)
Starting from April 2022, the Java analyzer offers rule developers a SQ cache that can be used to store and retrieve information from one analysis to the other.
The cache is provided by the underlying SonarQube instance and is branch-specific.
Please refer to the [sonar-java wiki](https://github.com/SonarSource/sonar-java/wiki/Cache-enabled-analysis) for additional information.


## Analyzing JSP and Thymeleaf for XSS vulnerabilities

In SonarQube Developer and Enterprise editions and on SonarCloud you can benefit from advanced security rules including XSS vulnerability detection. Java analysis supports analysis of Thymeleaf and JSP views when used with Java Servlets or Spring. To benefit from this analysis you need to make your views part of the project sources using `sonar.sources` property. In practice this usually means adding the following in your Maven `pom.xml` file

```xml
     <properties>
        <sonar.sources>src/main/java,src/main/webapp</sonar.sources>
      </properties>
```

or if you use Gradle
```groovy
    sonarqube {
    	properties {
    		property "sonar.sources", "src/main/java,src/main/webapp"
    	}
    }
```

where `src/main/webapp` is the directory which contains `.jsp` or Thymeleaf's `.html` files. 

## Related Pages

* [Test Coverage & Execution](/analysis/coverage/) (JaCoCo, Surefire)
* [Importing External Issues](/analysis/external-issues/) ([SpotBugs](https://spotbugs.github.io/), [FindBugs](http://findbugs.sourceforge.net/), [FindSecBugs](https://github.com/find-sec-bugs/find-sec-bugs/wiki/Maven-configuration), [PMD](http://maven.apache.org/plugins/maven-pmd-plugin/usage.html), [Checkstyle](http://maven.apache.org/plugins/maven-checkstyle-plugin/checkstyle-mojo))
<!-- sonarqube -->* [Adding Coding Rules](/extend/adding-coding-rules/)<!-- /sonarqube -->

<!-- sonarqube -->
## Custom Rules

The tutorial [Writing Custom Java Rules 101](https://redirect.sonarsource.com/doc/java-custom-rules-guide.html) will help to quickly start writing custom rules for Java.

### API changes

#### **7.16**
* New type: `RecordPatternTree`. Use this type to explore record patterns (Preview feature in Java 19).
* New method: `RecordPatternTree#type()`. Use this method to get the reference type in the record pattern (Preview feature in Java 19).
* New method: `RecordPatternTree#patterns()`. Use this method to get the patterns nested in the record pattern (Preview feature in Java 19).
* New method: `RecordPatternTree#name()`. Use this method get the optional record pattern identifier (Preview feature in Java 19).
* Dropped method: `GuardedPatternTree#andOperator()`. Use `GuardedPatternTree#whenOperator()` instead.
* New method: `GuardedPatternTree#whenOperator()` has now been replaced with the `whenOperator`  (Preview feature in Java 19).
* Deprecated method: `PatternInstanceOfTree#variable()`. Use `PatternInstanceOfTree#pattern` instead.
* New method: `PatternInstanceOfTree#variable()`. Use this method to get the pattern in an `instanceof` expression. When the pattern is a `TypePatternTree`, the variable can then be extracted using `TypePatternTree#patternVariable`.
* New method: `TreeVisitor#visitRecordPattern()`. Use this method to traverse a `RecordPatternTree`.

#### **7.15**
* New method: `JavaResourceLocator#binaryDirs()`. Use this method to get the directories containing the .class files corresponding to the main code.
* New method: `JavaResourceLocator#testBinaryDirs()`. Use this method to get the directories containing the .class files corresponding to the tests.
* New method: `JavaResourceLocator#testClasspath()`. Use this method to retrieve the classpath configured for the project's tests.

#### **7.12**

* New method: `JavaFileScanner#scanWithoutParsing(InputFileScannerContext)`. Use this method to inspect an unchanged file before it is parsed.
  This method allows you to pre-compute some work and even signal that the rule does not need the file to be parsed.
* `TypeArguments` extends `ListTree<TypeTree>` instead of `ListTree<Tree>`. This change does not impact the runtime
   compatibility but could break compile time compatibility in some rare cases.

#### **7.7**

* **Method `MethodSymbol.overriddenSymbol()` was dropped.**. Deprecated in 6.15 and planned to be dropped in 7.0, `overriddenSymbol()` has now been removed from the API. Use `MethodSymbol.overriddenSymbols()` instead.

#### **7.6**

* New method: `MethodSymbol.declarationParameters()`. Use this method to get the list of parameters symbols of this method. Placeholders symbols are created in case the declaration is not available (coming from an external dependency).

#### **7.4**

* New method: `JavaFileScannerContext.inAndroidContext()`. Use this method to know if the current file being analyzed is coming from an Android context. The value is true if Android dependencies are found in the classpath of the current analysis.

* Together with the previous method addition, you can use `CheckVerifier.withinAndroidContext(true)` in unit tests to test the behavior of the rules in an Android context.

#### **7.1**

* **Method `TypeCastTree.bounds()` changed its return type from `ListTree<Tree>` to `ListTree<TypeTree>`.**
  Having any kind of tree is not possible. Only "Typed" Trees are possible as bound of a cast. The change fixes the inconsistency.

* **Method `TypeParameterTree.bounds()` changed its return type from `ListTree<Tree>` to `ListTree<TypeTree>`.**
  Having any kind of tree is not possible. Only "Typed" Trees are possible as bound of a type parameter. The change fixes the inconsistency.

* **A new Tree Kind `PATTERN_INSTANCE_OF` has been formalized, with its new corresponding API class `PatternInstanceOfTree`.**
  The change follows Java 16 introduction of Pattern Matching for `instanceof` ([JEP-394](https://openjdk.java.net/jeps/394)).

* **A new Tree Kind `RECORD` has been formalized, adding a flavor to `ClassTree`.**
  The change follows Java 16 introduction of `record`s ([JEP-395](https://openjdk.java.net/jeps/395)).

#### **7.0**

* **Method `MethodTree.isOverriding()` now correctly match the contract in case of unknowns in the hierarchy.**
  Previously, the `isOverriding` implementation could return misleading results when the hierarchy of the class that the method belonged to contained unknowns. In such cases, null is now returned, because it is not possible to reliably determine if the method is an override or not.

The following deprecated methods have been dropped:

* **`org.sonar.plugins.java.api.JavaFileScannerContext.getFileKey()`**

  The method was deprecated for a long time and still used by Sonar-Security. It's not the case anymore, and the method can be dropped, without replacement.

* **`org.sonar.plugins.java.api.SourceMap.Location.inputFile()`**

  The method has been deprecated a few versions away and was only used by Sonar-Security. It's not used anymore, and the method can be dropped, without replacement.

* **`org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol.overriddenSymbol()`**

  The method has been replaced by `MethodSymbol.overriddenSymbols()`, which provide a list of overridden methods instead of only one.

* **`org.sonar.plugins.java.api.tree.BreakStatementTree.value()`**

  The method was deprecated in SonarJava 6.6. It was added to cover new switch expression of Java 12. The new switch expression was introduced officially in Java 14, using a new `yield` statement instead of relying on `break` statement, leading to the removal of this method.

* **`org.sonar.plugins.java.api.tree.CaseLabelTree.expression()`**

  The method was deprecated since SonarJava 5.12 when introducing support of Java 12, in favor of `CaseLabelTree.expressions()` method.

* **`org.sonar.plugins.java.api.tree.CaseLabelTree.colonToken()`**

  The method was deprecated since SonarJava 5.12 when introducing support of Java 12, in favor of `CaseLabelTree.colonOrArrowToken()` method.


* **`org.sonar.plugins.java.api.tree.SwitchStatementTree.asSwitchExpression()`**

  The method was making no sense and starting from SonarJava 6.15, switch expressions have their own distinct interface (`org.sonar.plugins.java.api.tree.SwitchExpressionTree`) and kind (`org.sonar.plugins.java.api.tree.Tree.Kind.SWITCH_EXPRESSION`).

The following classes have been dropped, since all their methods where already deprecated:

* Classes `org.sonar.java.checks.verifier.JavaCheckVerifier` and `org.sonar.java.checks.verifier.MultipleFilesJavaCheckVerifier`.

  All features are new achievable through `CheckVerifier.newVerifier()`.

#### **6.15**

* **Switch representation change in the AST**

  Previously, Switch Statements were represented thanks to a Switch Expression. It means that the child of the Switch
  Statement was a Switch Expression. This is no longer the case, a Switch statement is now a distinct node in the AST
  and does not contain a Switch Expression anymore. This may impact existing custom rules relying explicitly on Switch
  Expressions, via the kind SWITCH_EXPRESSION or the method visitSwitchExpression. More rarely, this could also impact
  rules relying on parents of Tree, as the overall shape of the AST may also change.

* **Deprecated**

    * `org.sonar.plugins.java.api.tree.SwitchStatementTree`: The `asSwitchExpression()` method is deprecated for removal.

    * `org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol`: The `overriddenSymbol()` method is deprecated for removal. It is replaced by method `overriddenSymbols()` which returns all the overridden symbols in the type hierarchy instead of only the first one found.

* **New interface `SwitchTree`**
  Switch Expression and Switch Statement share the same fields, it can sometimes make sense to manipulate a Switch as
  either one. Previously, it was possible to use the method asSwitchExpression() to do this. Since this method is
  deprecated, you can now use the new common interface `SwitchTree`, containing all the elements shared between Switch
  Expressions and Statements.

#### **6.3**

* API is now enriched with `MethodMatchers`. You can use it to identify a method with given a Type, Name and Parameters.
We realized that MethodMatchers is a really convenient way of writing new rules, it will hopefully ease the addition of rules in custom plugins, without having to rewrite the logic.
We are heavily using it in the different [checks](https://github.com/SonarSource/sonar-java/tree/master/java-checks/src/main/java/org/sonar/java/checks), plenty of examples can be found there.

* Two new methods have been added in the semantic API in order to access parametrized type in custom rules. The changes are available in `org.sonar.plugins.java.api.semantic.Type`:
     ```
     /**
      * Check if the current type is a parameterized type or not.
      *
      * @return true in case of Generic and Parameterized types
      *
      * @since SonarJava 6.3
      */
     boolean isParameterized();

     /**
      * The arguments of a parameterized type, as a parameterization of a generic type.
      *
      * @return the ordered list of type arguments. Returns an empty lists for non-parameterized types.
      *
      * @since SonarJava 6.3
      */
     List<Type> typeArguments();
     ```

* The [`JavaCheckVerifier`](https://github.com/SonarSource/sonar-java/tree/master/java-checks-testkit/src/main/java/org/sonar/java/checks/verifier/JavaCheckVerifier.java#L51), used to test rules implementations and delivered with the `java-checks-testkit` package, has been fully reworked in order to tackle inconcistencies. All the previously existing methods from it has been **deprecated**. In addition, a new method has been added, which allows access to a new rule testing interface. Starting from 6.3, when writting custom rules test, you should therefore rely only on `org.sonar.java.checks.verifier.JavaCheckVerifier.newVerifier()`. Example of change:
     ```
     // old test prior to 6.3:
     @Test
     public void deprecatedCustomRuleTest() {
        JavaCheckVerifier.verify("path/to/my/custom/check/test/file.java", new MyCheck());
     }

     // new test starting from 6.3:
     @Test
     public void newCustomRuleTest() {
        JavaCheckVerifier.newVerifier()
          .onFile("path/to/my/custom/check/test/file.java")
          .withCheck(new MyCheck())
          .verifyIssues();
     }
     ```

#### **6.1**

* The `ExpressionTree` interface, from the AST API, is now enriched by two new methods `Optional<Object> asConstant()` and `<T> Optional<T> asConstant(Class<T> type)`. These methods let you try to retrieve the equivalent constant value of an expression (from a variable, for instance). An example of usage would be:

```
class A {
  public static final String CONSTANT1 = "abc";
  public static final String CONSTANT2 = CONSTANT1 + "def";

  void foo() {
    System.out.println(CONSTANT2);
                    // ^^^^^^^^^ calling 'identifier.asConstant(String.class)' will return 'Optional.of("abcdef")'
  }
}
```

#### **6.0**

* Deprecated method `org.sonar.plugins.java.api.JavaFileScannerContext.addIssue(File, JavaCheck, int, String)` has been **removed**. Custom rules relying on it should report issues on a given `Tree` from now on.
* Deprecated method `org.sonar.plugins.java.api.JavaFileScannerContext.getFile()` has been **removed**. Custom rules relying on it should rely on content of SQ's API `InputFile`.
* Deprecated method `org.sonar.plugins.java.api.tree.TryStatementTree.resources()` has been **removed**, in favor of `org.sonar.plugins.java.api.tree.TryStatementTree.resourceList()`, as Java 9 allows other trees than `VariableTree` to be placed as resources in try-with-resources statements.
* Method `org.sonar.plugins.java.api.semantic.Symbol.owner()` has been **flagged** with `@Nullable` annotation, to explicitly document the fact that some symbols (package, unknown, recovered) might well return `null`.

* **Semantic engine**
    * Return type of constructor is now `void type` instead of `null`.
    * A **raw type** is now explicitly different from an **erasure type**. It is recommended to systematically use type erasure for type comparison when dealing with generics.
        ```
        class A<T> {
        //    ^^^^ Definition of a Generic Type
          boolean equals(Object o) {
            if (o instance of A) {
                           // ^ this is a raw type, not erasure of A<T>
             return true;
            }
            return false;
          }

          A<String> foo() {
            return new A<String>();
                   //  ^^^^^^^^^ Parameterization of a Generic Type
          }
        }
        ```
    * According to Java Language Specification every array type implements the interface `java.io.Serializable`, calling `isSubtypeOf("java.io.Serializable")` on an array type now consistently returns `true`.
    * Symbol corresponding to generic method invocations are now correctly parameterized.
    * In some special cases (mostly missing bytecode dependencies, misconfigured projects), and due to ECJ recovery system, unknown/recovered types can now lead to unknown symbols, even on `ClassTree`/`MethodTree`/`VariableTree`. To illustrate this, the following example now associate the method to an unknown symbol, while previous semantic engine from version 5.X series was creating a `Symbol.MethodSymbol` with an unknown return type.
        ```
        Class A {
          UnknownType<String> myMethod() { /* ... */ }
                          //  ^^^^^^^^  symbol corresponding to the MethodTree will be unknown,
        }
        ```
    * Thanks to improved semantic provided by ECJ engine, new semantic is now able to say that an *unknown* symbol is supposed to be type/variable/method (`isTypeSymbol()`, `isVariableSymbol()`, ...). Old semantic was answering `false` for all of them. Consequently, be sure to always use `isUnknown()` to validate symbol resolution. Other `is...Symbol()` methods are only designed to know how to cast the symbols (e.g from `Symbol` to `Symbol.MethodSymbol`).

#### **5.12**
* **Dropped**
    * `org.sonar.plugins.java.api.JavaFileScannerContext`: Drop deprecated method used to retrieve trees contributing to the complexity of a method from  (deprecated since SonarJava 4.1). 
        ```
        //org.sonar.plugins.java.api.JavaFileScannerContext
        /**
        * Computes the list of syntax nodes which are contributing to increase the complexity for the given methodTree.
        * @deprecated use {@link #getComplexityNodes(Tree)} instead
        * @param enclosingClass not used.
        * @param methodTree the methodTree to compute the complexity.
        * @return the list of syntax nodes incrementing the complexity.
        */
        @Deprecated
        List<Tree> getMethodComplexityNodes(ClassTree enclosingClass, MethodTree methodTree);
        ```
    * `org.sonar.plugins.java.api.JavaResourceLocator`: The following method has been dropped (deprecated since SonarJava 4.1), without replacement.
        ```
        //org.sonar.plugins.java.api.JavaResourceLocator
        /**
        * get source file key by class name.
        * @deprecated since 4.1 : will be dropped with no replacement.
        * @param className fully qualified name of the analyzed class.
        * @return key of the source file for the given class.
        */
        @Deprecated
        String findSourceFileKeyByClassName(String className);
        ```
    * `org.sonar.plugins.surefire.api.SurefireUtils`: Dropping deprecated field with old property (deprecated since SonarJava 4.11)
        ```
        //org.sonar.plugins.surefire.api.SurefireUtils
        /**
        * @deprecated since 4.11
        */
        @Deprecated
        public static final String SUREFIRE_REPORTS_PATH_PROPERTY = "sonar.junit.reportsPath";
        ```
* **Deprecated**  
    * `org.sonar.plugins.java.api.JavaFileScannerContext`: Deprecate usage of File-based methods from API, which will be removed in future release. Starting from this version, methods relying on InputFile has to be preferred.
        ```
        //org.sonar.plugins.java.api.JavaFileScannerContext
        /**
        * Report an issue at a specific line of a given file.
        * This method is used for one
        * @param file File on which to report
        * @param check The check raising the issue.
        * @param line line on which to report the issue
        * @param message Message to display to the user
        * @deprecated since SonarJava 5.12 - File are not supported anymore. Use corresponding 'reportIssue' methods, or directly at project level
        */
        @Deprecated
        void addIssue(File file, JavaCheck check, int line, String message);
        /**
        * FileKey of currently analyzed file.
        * @return the fileKey of the file currently analyzed.
        * @deprecated since SonarJava 5.12 - Rely on the InputFile key instead, using {@link #getInputFile()}
        */
        @Deprecated
        String getFileKey();

        /**
        * File under analysis.
        * @return the currently analyzed file.
        * @deprecated since SonarJava 5.12 - File are not supported anymore. Use {@link #getInputFile()} or {@link #getProject()} instead
        */
        @Deprecated
        File getFile();
        ```
    * Deprecate methods which are not relevant anymore in switch-related trees from API, following introduction of the new Java 12 `switch` expression:
        ```
        //org.sonar.plugins.java.api.tree.CaseLabelTree
        /**
        * @deprecated (since 5.12) use the {@link #expressions()} method.
        */
        @Deprecated
        @Nullable
        ExpressionTree expression();

        /**
        * @deprecated (since 5.12) use the {@link #colonOrArrowToken()} method.
        */
        @Deprecated
        SyntaxToken colonToken();
        ```
* **Added**
    * `org.sonar.plugins.java.api.JavaFileScannerContext`: Following methods have been added in order to provide help reporting issues at project level, and access data through SonarQube's InputFile API, which won't be possible anymore through files:
        ```
        //JavaFileScannerContext: New methods
        /**
        * Report an issue at at the project level.
        * @param check The check raising the issue.
        * @param message Message to display to the user
        */
        void addIssueOnProject(JavaCheck check, String message);

        /**
        * InputFile under analysis.
        * @return the currently analyzed inputFile.
        */
        InputFile getInputFile();

        /**
        * InputComponent representing the project being analyzed
        * @return the project component
        */
        InputComponent getProject();
        ```
    * In order to cover the Java 12 new switch expression, introduce a new Tree in the SonarJava Syntax Tree API  (Corresponding `Tree.Kind`: `SWITCH_EXPRESSION` ). New methods have also been added to fluently integrate the new switch expression into the SonarJava API.
        ```
        //org.sonar.plugins.java.api.tree.SwitchExpressionTree
        /**
        * 'switch' expression.
        *
        * JLS 14.11
        *
        * <pre>
        *   switch ( {@link #expression()} ) {
        *     {@link #cases()}
        *   }
        * </pre>
        *
        * @since Java 12
        */
        @Beta
        public interface SwitchExpressionTree extends ExpressionTree {
        
        SyntaxToken switchKeyword();
        
        SyntaxToken openParenToken();
        
        ExpressionTree expression();
        
        SyntaxToken closeParenToken();
        
        SyntaxToken openBraceToken();
        
        List<CaseGroupTree> cases();
        
        SyntaxToken closeBraceToken();
        }
        ```
        ```
        //org.sonar.plugins.java.api.tree.SwitchStatementTree
        /**
        * Switch expressions introduced with support Java 12
        * @since SonarJava 5.12
        */
        SwitchExpressionTree asSwitchExpression();
        ```
        ```
        //org.sonar.plugins.java.api.tree.CaseLabelTree
        /**
        * @return true for case with colon: "case 3:" or "default:"
        *         false for case with arrow: "case 3 ->" or "default ->"
        * @since 5.12 (Java 12 new features)
        */
        boolean isFallThrough();
        
        /**
        * @since 5.12 (Java 12 new features)
        */
        SyntaxToken colonOrArrowToken();
        ```
        ```
        //org.sonar.plugins.java.api.tree.BreakStatementTree
        /**
        * @since 5.12 (Java 12 new features)
        */
        @Nullable
        ExpressionTree value();
        ```
        ```
        //org.sonar.plugins.java.api.tree.TreeVisitor
        void visitSwitchExpression(SwitchExpressionTree tree);
        ```

#### **5.7**
* **Breaking**  
    * This change will impact mostly the custom rules relying on semantic API. The type returned by some symbols will change from raw type to parameterized type with identity substitution and this will change how subtyping will answer.

    It is possible to get the previous behavior back by using type erasure on the newly returned type. Note that not all returned types are impacted by this change.

    Example:
    ```
    @Rule(key = "MyFirstCustomRule")
    public class MyFirstCustomCheck extends IssuableSubscriptionVisitor {
    
        @Override
        public List<Kind> nodesToVisit() {
            return ImmutableList.of(Kind.METHOD);
        }
    
        @Override
        public void visitNode(Tree tree) {
            MethodTree method = (MethodTree) tree;
            MethodSymbol symbol = method.symbol();
            
            Type returnType = symbol.returnType().type();
            // When analyzing the code "MyClass<Integer> foo() {return null; }"
            // BEFORE: returnType == ClassJavaType
            // NOW: returnType == ParametrizedTypeJavaType
    
            // Getting back previous type
            Type erasedType = returnType.erasure();
            // erasedType == ClassJavaType
        }
    }
    ```
<!-- /sonarqube -->
