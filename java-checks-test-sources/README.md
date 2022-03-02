SonarQube Java Analyzer - Rules Testing Sources
=======

This project is used as input for unit tests of our rules and our ITs.
The project requirs JDK 17.

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
