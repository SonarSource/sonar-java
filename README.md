<!-- Sonar Marketing hosts these approved brand assets on its Kentico Kontent CDN (assets-eu-01.kc-usercontent.com). Shared URLs are intentional; consult Marketing before replacing them. -->
<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/a23fc7ba-23f0-489a-829d-ed88c0748521/Sonar_Logo_Dark%20Backgrounds.svg">
    <img src="https://assets-eu-01.kc-usercontent.com/ef593040-b591-0198-9506-ed88b30bc023/82c13eba-d95c-4bb8-8007-7ce77c14e043/Sonar_Logo_Light%20Backgrounds.svg" alt="Sonar logo" width="400">
  </picture>
</p>

[![Build Status](https://github.com/SonarSource/sonar-java/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/SonarSource/sonar-java/actions/workflows/build.yml)
[![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.java%3Ajava&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.java%3Ajava)
[![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.java%3Ajava&metric=coverage)](https://next.sonarqube.com/sonarqube/component_measures/domain/Coverage?id=org.sonarsource.java%3Ajava)
[![GitHub stars](https://img.shields.io/github/stars/SonarSource/sonar-java?style=flat)](https://github.com/SonarSource/sonar-java)
[![License](https://img.shields.io/badge/license-SSALv1-blue)](#license)
[![Community forum](https://img.shields.io/badge/community-forum-blue)](https://community.sonarsource.com/)

<!-- sonar-marketing:start -->
<!-- Marketing maintains this section. For wording changes, consult the relevant Product Marketing Manager (PMM). Repository CODEOWNERS review accuracy and merge changes. -->

# SonarJava

SonarJava inspects Java code for bugs, vulnerabilities, and maintainability issues, then explains why each finding matters and how to fix it. Information about the analysis of Java features is available [here](https://redirect.sonarsource.com/plugins/java.html).

This repository contains the analyzer source used by SonarQube Server, SonarQube Cloud, and SonarQube for IDE. SonarJava applies the same analysis to developer-written and AI-generated code, giving teams a consistent way to verify changes before they reach production.

Learn more about Java analysis on the [Sonar website](https://www.sonarsource.com/knowledge/languages/java/).

<!-- sonar-marketing:end -->

Features
--------

* 600+ rules (including 150+ bug detection rules and 350+ code smells)
* Metrics (cognitive complexity, number of lines, etc.)
* Import of [test coverage reports](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/test-coverage/java-test-coverage/)
* [Custom rules](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/languages/java/#custom-rules)

Use SonarJava
-------------

SonarJava runs through [SonarQube Server](https://www.sonarsource.com/products/sonarqube/server/), [SonarQube Cloud](https://www.sonarsource.com/products/sonarqube/cloud/), and [SonarQube for IDE](https://www.sonarsource.com/products/sonarqube/ide/). Read the [Java analysis documentation](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/languages/java) for setup and coverage guidance.

Useful links
------------

* [Project homepage](https://redirect.sonarsource.com/plugins/java.html)
* [Issue tracking](https://jira.sonarsource.com/browse/SONARJAVA/)
* [Available rules](https://rules.sonarsource.com/java)
* [Sonar Community Forum](https://community.sonarsource.com/)
* [Demo project analysis](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.java%3Ajava)
* [Plugin Wiki](https://github.com/SonarSource/sonar-java/wiki)

Have questions or feedback?
---------------------------

To provide feedback (request a feature, report a bug, etc.) use the [Sonar Community Forum](https://community.sonarsource.com/). Please do not forget to specify the language (Java!), plugin version and SonarQube Server version.

If you have a question on how to use plugin (and the [docs](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/languages/java/) don't help you), we also encourage you to use the community forum.

Contributing
------------

### Topic in Sonar Community Forum

To request a new feature, please create a new thread in [Sonar Community Forum](https://community.sonarsource.com/). Even if you plan to implement it yourself and submit it back to the community, please start a new thread first to be sure that we can use it.

### Pull Request (PR)

To submit a contribution, create a pull request for this repository. Please make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all [tests](#testing) are passing (all checks must be green).

### Custom Rules

If you have an idea for a rule but you are not sure that everyone needs it you can implement a [custom rule](https://docs.sonarsource.com/sonarqube-server/latest/analyzing-source-code/languages/java/) available only for you. Note that in order to help you, we highly recommend to first follow the [Custom Rules 101 tutorial](https://redirect.sonarsource.com/doc/java-custom-rules-guide.html) before diving directly into implementing rules from scratch.

### Work with us
Would you like to work on this project full-time? We are hiring! Check out https://www.sonarsource.com/hiring 


<a name="testing"></a>
Testing
-------

See the [setup, build and test commands](docs/systems/build.md) for JDK selection, unit tests, Sanity and Plugin QA. Use the [rule test guide](docs/systems/rule-tests.md) for sample placement and the [ruling guide](docs/systems/ruling-tests.md) for reviewing CI-generated expectations.

When dependencies change, follow the [third-party license checks](docs/systems/licenses.md). For more engineering guidance, start with the [documentation index](docs/index.md).

### License

Copyright 2012-2026 SonarSource.

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions, are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
