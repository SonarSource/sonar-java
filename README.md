Sonar Java - Custom build
=========================

Compiled against SonarQube 4.2

Supports Java 8 projects (01/04/2014)

### Compile Findbugs 3.0.0-dev-20140323-8036a5d9

~~~
git clone https://code.google.com/p/findbugs
cd findbugs
ant
~~~

--> this will put libs into `findbugs/findbugs/lib`. You need the
`findbugs.jar` and `bcel-6.0-SNAPSHOT.jar` (this one, and no other!).

### Compile Sonar Java Ecosystem 2.2-SNAPSHOT

Clone this repository, then:

~~~
cd sonar-java
mvn package -DskipTests
~~~

Search for `sonar-*SHOT.jar` and pick:

~~~
sonar-findbugs-plugin-2.2-SNAPSHOT.jar
sonar-jacoco-plugin-2.2-SNAPSHOT.jar
sonar-java-plugin-2.2-SNAPSHOT.jar
sonar-squid-java-plugin-2.2-SNAPSHOT.jar
sonar-surefire-plugin-2.2-SNAPSHOT.jar
~~~
