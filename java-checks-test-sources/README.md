SonarQube Java Analyzer - Rules Testing Sources
=======

This project is used as input for unit tests of our rules and our ITs.
The project requires JDK 21.

## When to put samples here

Use this module for rule test samples that are meant to behave like regular Maven sources.
In `java-checks` tests, this typically means files loaded through `TestUtils.mainCodeSourcesPath(...)`,
`TestUtils.testCodeSourcesPath(...)`, or `TestUtils.nonCompilingTestSourcesPath(...)`.

This is the right place when a sample:

* needs the classpath or dependencies prepared by `java-checks-test-sources`
* should be compiled as part of one of the dedicated test-source modules
* is intentionally non-compiling and should live under `src/main/files/non-compiling`
* targets a specific dedicated module such as `default`, `java-17`, `spring-3.2`, or `spring-web-4.0`

Prefer `java-checks/src/test/files` only for fixtures that are not supposed to belong to one of these Maven modules,
for example parser-only inputs or other ad hoc verifier fixtures.

To analyze it with a **local instance of SonarQube** up and running, use the following command from its root:
```
mvn clean install sonar:sonar -Panalyze-tests
```

You can also use the project to test the behavior of sonar-scanner with various configuration, as long as you have a **local instance of SonarQube** up and running:

* Testing how sonar-scanner combined with the Java Analyzer behave when no-binaries are provided at all (dependencies and compiled classes of the projects no provided)

```
sonar-scanner -Dproject.settings=sonar-project-no-binaries.properties
```

* Testing how sonar-scanner combined with the Java Analyzer behave when binaries are provided:

```
sonar-scanner -Dproject.settings=sonar-project-with-binaries.properties
```
