---
title: Writing Custom Java Rules 101
description: Learn to write, test, and deploy a custom rule for the SonarQube Java Analyzer.
generated:
  by: tirion
---

# Writing Custom Java Rules 101

If the built-in Java rules do not cover your team's requirements, you can write your own using the SonarQube Java Analyzer API. This tutorial takes you from a rule specification and a failing test to a working rule in a custom plugin.

## Tutorial

1. [Getting started](getting-started.md): import the example project and choose the Maven configuration for your target server (`docs/custom-rules/getting-started.md`).
2. [Writing a rule](writing-a-rule.md): specify the rule and create a test that fails before implementation (`docs/custom-rules/writing-a-rule.md`).
3. [Implementing the rule](implementing-the-rule.md): use syntax trees and the semantic API to make the test pass (`docs/custom-rules/implementing-the-rule.md`).
4. [Registering the rule in the custom plugin](registering-the-rule.md): add metadata, register the rule, and update the registration test (`docs/custom-rules/registering-the-rule.md`).
5. [Testing a custom plugin](testing-a-custom-plugin.md): deploy the plugin to SonarQube and activate the rule, or look up additional rule testing techniques (`docs/custom-rules/testing-a-custom-plugin.md`).

The [example project](../java-custom-rules-example/index.md) in `docs/java-custom-rules-example` contains working custom rules. The [original tutorial URL](../CUSTOM_RULES_101.md) still provides links for older section bookmarks (`docs/CUSTOM_RULES_101.md`).

## References

- [Analysis of Java code documentation](https://docs.sonarqube.org/latest/analysis/languages/java/)
- [SonarQube Platform](http://www.sonarqube.org/)
- [SonarSource Code Quality and Security for Java GitHub Repository](https://github.com/SonarSource/sonar-java)
- [SonarQube Java Custom-Rules Example (SonarJava 7.16)](https://github.com/SonarSource/sonar-java/tree/7.16.0.30901/docs/java-custom-rules-example)
