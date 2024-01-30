Code Quality and Security for Java [![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-java.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-java) [![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.java%3Ajava&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.java%3Ajava) [![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.java%3Ajava&metric=coverage)](https://next.sonarqube.com/sonarqube/component_measures/domain/Coverage?id=org.sonarsource.java%3Ajava)
==========

This SonarSource project is a code analyzer for Java projects to help developers produce [Clean Code](https://www.sonarsource.com/solutions/clean-code/). Information about the analysis of Java features is available [here](https://redirect.sonarsource.com/plugins/java.html).

Features
--------

* 600+ rules (including 150+ bug detection rules and 350+ code smells)
* Metrics (cognitive complexity, number of lines, etc.)
* Import of [test coverage reports](https://docs.sonarqube.org/display/PLUG/Code+Coverage+by+Unit+Tests+for+Java+Project)
* [Custom rules](https://docs.sonarqube.org/latest/analysis/languages/java/)

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

To provide feedback (request a feature, report a bug, etc.) use the [Sonar Community Forum](https://community.sonarsource.com/). Please do not forget to specify the language (Java!), plugin version and SonarQube version.

If you have a question on how to use plugin (and the [docs](https://docs.sonarqube.org/latest/analysis/languages/java/) don't help you), we also encourage you to use the community forum.

Contributing
------------

### Topic in SonarQube Community Forum

To request a new feature, please create a new thread in [SonarQube Community Forum](https://community.sonarsource.com/). Even if you plan to implement it yourself and submit it back to the community, please start a new thread first to be sure that we can use it.

### Pull Request (PR)

To submit a contribution, create a pull request for this repository. Please make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all [tests](#testing) are passing (all checks must be green).

### Custom Rules

If you have an idea for a rule but you are not sure that everyone needs it you can implement a [custom rule](https://docs.sonarqube.org/latest/analysis/languages/java/) available only for you. Note that in order to help you, we highly recommend to first follow the [Custom Rules 101 tutorial](https://redirect.sonarsource.com/doc/java-custom-rules-guide.html) before diving directly into implementing rules from scratch.

### Work with us
Would you like to work on this project full-time? We are hiring! Check out https://www.sonarsource.com/hiring 


<a name="testing"></a>
Testing
-------

To run tests locally follow these instructions.

### Java versions

You need `Java 21` to build the project and `Java 17` run the Integration Tests (ITs).
* `Java 17` can be used to build and test all modules except under `java-checks-test-sources` that requires `Java 21`.
* `Java 21` can be used to build and test all modules except under `its` that requires `Java 17` because of SQ imcompatibility.

### Build the Project and Run Unit Tests

To build the plugin and run its unit tests, execute this command from the project's root directory:

    mvn clean install

### Note that
Running unit tests within the IDE might incur in some issues because of the way the project is built with Maven.
If you see something like this:
	
	java.lang.SecurityException: class ... signer information does not match signer information of other classes in the same package
	
try removing the Maven nature of the 'jdt' module. 

### Integration Tests

To run integration tests, you will need to create a properties file like the one shown below, and set the URL pointing to its location in an environment variable named `ORCHESTRATOR_CONFIG_URL`.

    # version of SonarQube Server
    sonar.runtimeVersion=7.9

    orchestrator.updateCenterUrl=http://update.sonarsource.org/update-center-dev.properties

    # The location of the Maven local repository is not automatically guessed. It can also be set with the env variable MAVEN_LOCAL_REPOSITORY.
    maven.localRepository=/home/myName/.m2/repository

With for instance the `ORCHESTRATOR_CONFIG_URL` variable being set as: 

    export ORCHESTRATOR_CONFIG_URL=file:///home/user/workspace/orchestrator.properties

Before running the ITs, be sure your MAVEN_HOME environment variable is set.

#### Sanity Test

The "Sanity Test" is a test that runs all checks against all the test source files without taking into account the result of the analysis. It verifies that rules are not crashing on any file in our test sources. By default, this test is excluded from the build. To launch it:

    mvn clean install -P sanity

#### Plugin Test

The "Plugin Test" is an integration test suite that verifies plugin features such as metric calculation, coverage, etc. To launch it:

    mvn clean install -Pit-plugin -DcommunityEditionTestsOnly=true

Note for internal contributors: in order to also execute the tests that depend on the SonarQube Enterprise Edition, use:

    mvn clean install -Pit-plugin

#### Ruling Test

The "Ruling Test" is an integration test suite that launches the analysis of a large code base, saves the issues created by the plugin in report files, and then compares those results to the set of expected issues (stored as JSON files).

To run the test, first make sure the submodules are checked out:

    git submodule update --init --recursive

Then, ensure that the `JAVA_HOME` environment variable is set for the ruling tests execution and that it points to your local JDK 17 installation.
Failing to do so will produce inconsistencies with the expected results.

From the `its/ruling` folder, launch the ruling tests:

    mvn clean install -Pit-ruling -DcommunityEditionTestsOnly=true 
    # Alternatively
    JAVA_HOME=/my/local/java17/jdk/ mvn clean install -Pit-ruling -DcommunityEditionTestsOnly=true

Note for internal contributors: in order to also execute the tests that depend on the SonarQube Enterprise Edition, use: 

    mvn clean install -Pit-ruling

This test gives you the opportunity to examine the issues created by each rule and make sure they're what you expect. Any implemented rule is highly likely to raise issues on the multiple projects we use as ruling code base.

* For a newly implemented rule, it means that a first build will most probably fail, caused by differences between expected results (without any values for the new rule) and the new results. You can inspect these new issues by searching for files named after your rule (`squid-SXXXX.json`) in the following folder:

        /path/to/project/sonar-java/its/ruling/target/actual/...

* For existing rules which are modified, you may expect some differences between "actual" (from new analysis) and expected results. Review carefully the changes that are shown and update the expected resources accordingly.

All the `json` files contain a list of lines, indexed by file, explaining where the issues raised by a specific rule are located. If/When everything looks good to you, you can copy the file with the actual issues located at:

    its/ruling/target/actual/

Into the directory with the expected issues:

    its/ruling/src/test/resources/

For example using the command:

    cp its/ruling/target/actual/* its/ruling/src/test/resources/

#### Autoscan Test

The tests in the [autoscan module](its/autoscan) are designed to detect differences between the issues the Java analyzer can find with and without bytecode.
The goal here is to spot and fix the potential FPs, and verify the expected FNs between that would show up in [SonarCloud's automatic analysis](https://docs.sonarcloud.io/advanced-setup/automatic-analysis/).

Running this test can be broken down in 2 steps:

1. Compiling the test sources
2. Executing the autoscan test

##### Compiling the test sources

Make sure that the `java-checks-tests-sources` module has been compiled (ie: the .class files in `java-checks-tests-sources/target/` are up to date).

In doubt, go the top-level of the project and run:
```shell
mvn clean compile --projects java-checks-test-sources --also-make-dependents
```

##### Executing the autoscan test

To run the tests, move to the [`its/autoscan`](its/autoscan) folder and run:
```shell
# cd its/autoscan
mvn clean package --batch-mode --errors --show-version \
   --activate-profiles it-autoscan \
  -Dsonar.runtimeVersion=LATEST_RELEASE[9.9]
```

The artifacts produced during the test execution will be found in [`its/autoscan/target/actual`](its/autoscan/target/actual).
You will want to compare the results produced in the autoscan-diff-by-rules

For more detailed information, you can compare the differences between the results found with bytecode and without bytecode by comparing two respective folders:
* [java-checks-test-sources-mvn](its/autoscan/target/actual/java-checks-test-sources-mvn)
* [java-checks-test-sources-no-binaries](its/autoscan/target/actual/java-checks-test-sources-no-binaries)

Depending on the results found, you might need to update the ground truth.
The expected results are listed in [src/test/resources](its/autoscan/src/test/resources/autoscan/).

#### Debugging Integration Tests
You can debug ITs by adding `-Dmaven.binary=mvnDebug` as an option when running the tests. This will cause the analyzer JVM to wait for a debugger to be attached before continuing.

### License

Copyright 2012-2024 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
