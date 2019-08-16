SonarJava [![Build Status](https://travis-ci.org/SonarSource/sonar-java.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-java) [![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.java%3Ajava&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.java%3Ajava) [![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.java%3Ajava&metric=coverage)](https://next.sonarqube.com/sonarqube/component_measures/domain/Coverage?id=org.sonarsource.java%3Ajava)
==========

SonarJava is a code analyzer for Java projects. Information about the SonarJava features is available [here](https://redirect.sonarsource.com/plugins/java.html).

**Build status (all branches)**

[![Build Status](https://api.travis-ci.org/SonarSource/sonar-java.svg)](https://travis-ci.org/SonarSource/sonar-java)

Features
--------

* 430+ rules (including 140+ bug detection)
* Metrics (complexity, number of lines etc.)
* Import of [test coverage reports](https://docs.sonarqube.org/display/PLUG/Code+Coverage+by+Unit+Tests+for+Java+Project)
* [Custom rules](https://docs.sonarqube.org/latest/analysis/languages/java/)

Useful links
------------

* [Project homepage](https://redirect.sonarsource.com/plugins/java.html)
* [Issue tracking](https://jira.sonarsource.com/browse/SONARJAVA/)
* [Available rules](https://rules.sonarsource.com/java)
* [SonarQube Community Forum](https://community.sonarsource.com/)
* [Demo project analysis](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.java%3Ajava)

Have question or feedback?
--------------------------

To provide feedback (request a feature, report a bug etc.) use the [SonarQube Community Forum](https://community.sonarsource.com/). Please do not forget to specify the language (Java!), plugin version and SonarQube version.

If you have a question on how to use plugin (and the [docs](https://docs.sonarqube.org/display/PLUG/SonarJava) don't help you), we also encourage you to use the community forum.

Contributing
------------

### Topic in SonarQube Community Forum

To request a new feature, please create a new thread in [SonarQube Community Forum](https://community.sonarsource.com/). Even if you plan to implement it yourself and submit it back to the community, please start a new thread first to be sure that we can use it.

### Pull Request (PR)

To submit a contribution, create a pull request for this repository. Please make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all [tests](#testing) are passing (Travis build is created for each PR).

### Custom Rules

If you have an idea for a rule but you are not sure that everyone needs it you can implement a [custom rule](https://docs.sonarqube.org/latest/analysis/languages/java/) available only for you. Note that in order to help you, we highly recommend to first follow the [Custom Rules 101 tutorial](https://redirect.sonarsource.com/doc/java-custom-rules-guide.html) before diving directly into implementing rules from scratch.

<a name="testing"></a>
Testing
-------

To run tests locally follow these instructions.

### Build the Project and Run Unit Tests

To build the plugin and run its unit tests, execute this command from the project's root directory:

    mvn clean install

### Integration Tests

To run integration tests, you will need to create a properties file like the one shown below, and set the url pointing to its location in an environment variable named `ORCHESTRATOR_CONFIG_URL`.

    # version of SonarQube Server
    sonar.runtimeVersion=7.9

    orchestrator.updateCenterUrl=http://update.sonarsource.org/update-center-dev.properties
    
    # Location of Maven local repository is not automatically guessed. It can also be set with the env variable MAVEN_LOCAL_REPOSITORY.
    maven.localRepository=/home/myName/.m2/repository

With for instance the `ORCHESTRATOR_CONFIG_URL` variable being set as: 

    export ORCHESTRATOR_CONFIG_URL=file:///home/user/workspace/orchestrator.properties

Before running the ITs, be sure your MAVEN_HOME environment variable is set.

#### Plugin Test

The "Plugin Test" is an integration test suite which verifies plugin features such as metric calculation, coverage etc. To launch it:

    mvn clean install -Pit-plugin

#### Ruling Test

The "Ruling Test" are an integration test suite which launches the analysis of a large code base, saves the issues created by the plugin in report files, and then compares those results to the set of expected issues (stored as JSON files).

To run the test, first make sure the submodules are checked out:

    git submodule init 
    git submodule update

Launch ruling test:

    cd its/ruling
    mvn clean install -DskipTests=false

This test gives you the opportunity to examine the issues created by each rule and make sure they're what you expect. Any implemented rule is highly likely to raise issues on the multiple projects we use as ruling code base.

* For newly implemented rule, it means that a first build will most probably fail, caused by differences between expected results (without any values for the new rule) and the new results. You can inspect these new issues by searching for files named after your rule (`squid-SXXXX.json`) in the following folder:

        /path/to/project/sonar-java/its/ruling/target/actual/...

* For existing rules which are modified, you may expect some differences between "actual" (from new analysis) and expected results. Review carefully the changes which are shown and update the expected resources accordingly.

All the  `json` files contain a list of lines, indexed by file, expliciting where the issues raised by a specific rule are located. If/When everything looks good to you, you can copy the file with the actual issues located at:

    its/ruling/target/actual/

Into the directory with the expected issues:

    its/ruling/src/test/resources/

For example using the command:

    cp its/ruling/target/actual/* its/ruling/src/test/resources/

### License

Copyright 2012-2019 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](https://www.gnu.org/licenses/lgpl.txt)
