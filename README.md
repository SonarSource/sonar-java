SonarJava [![Build Status](https://travis-ci.org/SonarSource/sonar-java.svg?branch=master)](https://travis-ci.org/SonarSource/sonar-java) 
==========

SonarJava is a code analyzer for Java projects. Information about the SonarJava features is available [here](https://www.sonarsource.com/why-us/products/languages/java.html).

**Build status (all branches)**

[![Build Status](https://api.travis-ci.org/SonarSource/sonar-java.svg)](https://travis-ci.org/SonarSource/sonar-java)

Features
--------

* 370+ rules (including 80+ bug detection)
* Metrics (complexity, number of lines etc.)
* Import of [test coverage reports](http://docs.sonarqube.org/display/PLUG/Code+Coverage+by+Unit+Tests+for+Java+Project)
* [Custom rules](http://docs.sonarqube.org/display/DEV/Custom+Rules+for+Java)

Useful links
------------

* [Project homepage](http://docs.sonarqube.org/display/PLUG/Java+Plugin)
* [Issue tracking](http://jira.sonarsource.com/browse/SONARJAVA)
* [Available rules](https://nemo.sonarqube.org/coding_rules#languages=java|repositories=squid)
* [Google Group for feedback](https://groups.google.com/forum/#!forum/sonarqube)
* [Demo project analysis](https://nemo.sonarqube.org/overview?id=org.sonarsource.sonarqube%3Asonarqube)

Have question or feedback?
--------------------------

To provide feedback (request a feature, report a bug etc.) use the [SonarQube Google Group](https://groups.google.com/forum/#!forum/sonarqube). Please do not forget to specify the language (Java!), plugin version and SonarQube version.
If you have a question on how to use plugin (and the [docs](http://docs.sonarqube.org/display/PLUG/Java+Plugin) don't help you) direct it to [StackOverflow](http://stackoverflow.com/questions/tagged/sonarqube+java) tagged both `sonarqube` and `java`.

Contributing
------------

### Topic in SonarQube Google Group

To request a new feature, please create a new thread in [SonarQube Google Group](https://groups.google.com/forum/#!forum/sonarqube). Even if you plan to implement it yourself and submit it back to the community, please start a new thread first to be sure that we can use it.

### Pull Request (PR)

To submit a contribution, create a pull request for this repository. Please make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all [tests](#testing) are passing (Travis build is created for each PR).

### Custom Rules

If you have an idea for a rule but you are not sure that everyone needs it you can implement a [custom rule](http://docs.sonarqube.org/x/hQBJ) available only for you.

<a name="testing"></a>
Testing
-------

To run tests locally follow these instructions.

### Build the Project and Run Unit Tests

To build the plugin and run its unit tests, execute this command from the project's root directory:

    mvn clean package

Note: You need to run the `package` goal because the jacoco-previous module will shade JaCoCo to support two binary format of JaCoCo. This shading mechanism is bound to maven `package` phase and is required for other modules to compile.

### Integration Tests

To run integration tests, you will need to create a properties file like the one shown below, and set the url pointing to its location in an environment variable named `ORCHESTRATOR_CONFIG_URL`.

    # version of SonarQube Server
    sonar.runtimeVersion=5.6

    orchestrator.updateCenterUrl=http://update.sonarsource.org/update-center-dev.properties

With for instance the `ORCHESTRATOR_CONFIG_URL` variable being set as: 

    ORCHESTRATOR_CONFIG_URL=file:///home/user/workspace/orchestrator.properties

#### Plugin Test

The "Plugin Test" is an additional integration test which verifies plugin features such as metric calculation, coverage etc. To launch it:

    mvn clean install -Pit-plugin

#### Ruling Test

The "Ruling Test" is a special integration test which launches the analysis of a large code base, saves the issues created by the plugin in report files, and then compares those results to the set of expected issues (stored as JSON files).

To run the test, first make sure the submodules are checked out:

    git submodule init 
    git submodule update

Launch ruling test:

    cd its/ruling
    mvn clean install -DskipTests=false

This test gives you the opportunity to examine the issues created by each rule and make sure they're what you expect. You can inspect new/lost issues checking web-pages mentioned in the logs at the end of analysis:

    INFO  - HTML Issues Report generated: /path/to/project/sonar-java/its/sources/src/.sonar/issues-report/issues-report.html
    INFO  - Light HTML Issues Report generated: /path/to/project/sonar-java/its/sources/src/.sonar/issues-report/issues-report-light.html

If everything looks good to you, you can copy the file with the actual issues located at:

    its/ruling/target/actual/

Into the directory with the expected issues:

    its/ruling/src/test/resources/

For example using the command:

    cp its/ruling/target/actual/* its/ruling/src/test/resources/

### License

Copyright 2012-2017 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)