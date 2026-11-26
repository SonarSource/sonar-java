---
title: Getting started with custom Java rules
description: Set up the Maven example project and identify the plugin settings that need to match your SonarQube server.
generated:
  by: tirion
---

# Getting started

Your rules will be delivered in a dedicated plugin that uses the SonarQube Java Analyzer API. Clone [this repository](https://github.com/SonarSource/sonar-java) and import the [example Maven project](https://github.com/SonarSource/sonar-java/tree/master/docs/java-custom-rules-example) into your IDE. The project already contains working custom rules. In this tutorial, you'll add another one.

## Looking at the POM

The [example project](../java-custom-rules-example/index.md) has two Maven POMs:

- [`pom.xml`](../java-custom-rules-example/pom.xml) is part of the SonarJava reactor build and uses its snapshot of the Java Analyzer. It compiles the example with Java 21.
- [`pom_SQ_10_6_LATEST.xml`](../java-custom-rules-example/pom_SQ_10_6_LATEST.xml) builds independently of the reactor and pins the dependencies for the SonarQube 10.6 example. Despite the filename, it does not track the latest SonarQube release.

This tutorial uses the standalone POM. Use Maven with JDK 21 for the tutorial; the standalone POM targets Java 8 bytecode, which is separate from the JDK used to run the build. From the repository root, run:

```sh
cd docs/java-custom-rules-example
mvn clean install -f pom_SQ_10_6_LATEST.xml
```

Keep both POMs in place. Use `-f pom_SQ_10_6_LATEST.xml` when running Maven commands for the standalone example, or select that POM when importing the project into your IDE.

SonarQube ships with a specific version of the Java Analyzer. For example, SonarQube 8.9 shipped with version `6.15.1.26025`, and SonarQube 10.6 shipped with version `8.0.1.36337`. When building for a different SonarQube release, check which Java Analyzer and plugin API versions it supports before changing the pinned dependencies; do not assume the 10.6 example works unchanged.

```xml
<properties>
  <sonar.plugin.api.version>10.7.0.2191</sonar.plugin.api.version>
  <sonarjava.version>8.0.1.36337</sonarjava.version>
  <!-- [...] -->
</properties>
```

You can customize tags such as `<groupId>`, `<artifactId>`, `<version>`, `<name>`, and `<description>`:

```xml
<groupId>org.sonar.samples</groupId>
<artifactId>java-custom-rules-example</artifactId>
<version>1.0.0-SNAPSHOT</version>
<name>SonarQube Java :: Documentation :: Custom Rules Example</name>
<description>Java Custom Rules Example for SonarQube</description>
```

The plugin's entry point is `<pluginClass>` in the `sonar-packaging-maven-plugin` configuration. Change it if you rename or move `MyJavaRulesPlugin`, the class that implements `org.sonar.api.Plugin`.

Starting with SonarJava 7.30, set `<requiredForLanguages>` to `java`. This ensures that the custom plugin loads only when the analyzed project contains Java code, so it does not try to initialize without the Java Analyzer.

```xml
<plugin>
  <groupId>org.sonarsource.sonar-packaging-maven-plugin</groupId>
  <artifactId>sonar-packaging-maven-plugin</artifactId>
  <extensions>true</extensions>
  <configuration>
    <pluginKey>java-custom</pluginKey>
    <pluginName>Java Custom Rules</pluginName>
    <pluginClass>org.sonar.samples.java.MyJavaRulesPlugin</pluginClass>
    <sonarLintSupported>true</sonarLintSupported>
    <skipDependenciesPackaging>true</skipDependenciesPackaging>
    <requirePlugins>java:${sonarjava.version}</requirePlugins>
    <requiredForLanguages>java</requiredForLanguages>
  </configuration>
</plugin>
```

Next: [Write a rule specification and a failing test](writing-a-rule.md) · [Tutorial index](index.md)
