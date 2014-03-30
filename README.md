Sonar Java - Custom build
=========================

Supports Java 8 projects (29/04/2014)

### Compile Findbugs 3.0.0-dev-20140323-8036a5d9

~~~
git clone https://code.google.com/p/findbugs
ant
~~~

--> this will put libs into `u:\src\findbugs\findbugs\lib\`

set `FINDBUGS_HOME=u:\src\findbugs\findbugs`


### Compile Sonar Java Ecosystem 2.2-SNAPSHOT

Original source at: https://github.com/SonarSource/sonar-java

Did some changes to make it work with Jacoco, Findbugs, etc. So here it is:

~~~
git clone git@github.com:tischda/sonar-java.git
cd sonar-java
mvn package -DskipTests
~~~

Compile with `skipTests` because tests won't find the findbugs dependency. You
can fix the `pom.xml` to run the tests (but then package fails to add the
required `findbugs.jar` dependency).

Search for `sonar-*SHOT.jar` and pick:

~~~
sonar-findbugs-plugin-2.2-SNAPSHOT.jar
sonar-jacoco-plugin-2.2-SNAPSHOT.jar
sonar-java-plugin-2.2-SNAPSHOT.jar
sonar-squid-java-plugin-2.2-SNAPSHOT.jar
sonar-surefire-plugin-2.2-SNAPSHOT.jar
~~~


### Compile SonarQube 4.3-SNAPSHOT (optional)

~~~
git clone https://github.com/SonarSource/sonarqube.git
~~~

If you want, edit `pom.xml` to update version of Tomcat and MySQL connector:

~~~xml
<tomcat.version>7.0.52</tomcat.version>
...
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.29</version>
</dependency>
~~~

Compile and package:

~~~
mvn package -Dmaven.test.failure.ignore=true
~~~

--> find package in: `./sonar-application/target`


### Customize package (optional)

Open `.\sonar-application\target\sonarqube-4.3-SNAPSHOT.zip` and add plugins to
`sonarqube-4.3-SNAPSHOT\extensions\plugins`

Remove `sonarqube-4.3-SNAPSHOT\lib\bundled-plugins\*`


Installation
------------

Unzip into `/home/sonar/sonar`

Edit `conf/sonar.properties`:

~~~
sonar.jdbc.password=<password>
sonar.jdbc.url=jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true
sonar.jdbc.driverClassName=com.mysql.jdbc.Driver
~~~


Uncomment and set:

~~~
sonar.web.context=/sonar
sonar.web.port=9000

sonar.web.http.maxThreads=50
sonar.web.https.maxThreads=50
sonar.web.http.acceptCount=25
sonar.web.https.acceptCount=25

sonar.updatecenter.activate=false
~~~


Edit `conf/wrapper.conf`:

~~~
wrapper.java.additional.2=-XX:MaxPermSize=256m
wrapper.java.additional.6=-XX:+UseG1GC
wrapper.java.additional.7=-server
wrapper.java.initmemory=512
wrapper.java.maxmemory=1024
~~~

This is the critical part:

~~~
wrapper.java.command=/usr/java/jdk1.7.0_51/bin/java
~~~


Edit `/home/sonar/sonar/bin/linux-x86-64/sonar.sh`

~~~
PIDDIR="/var/run/sonar"
RUN_AS_USER=sonar
# chkconfig: 2345 70 20
~~~


Link back from `/etc`

~~~
cd /etc/sonar
mv /home/sonar/sonar/conf/* .
cd /home/sonar/sonar/conf
ln -s /etc/sonar/sonar.properties
ln -s /etc/sonar/wrapper.conf
~~~

Link back to `sonar.sh` service script:

~~~
cd /etc/rc.d/init.d
ln -s /home/sonar/sonar/bin/linux-x86-64/sonar.sh sonar
~~~


### Start Sonar & upgrade database

~~~html
<form action="/setup/setup_database" method="POST">
<div class="admin migration">
  <h1 class="marginbottom10">Upgrade database</h1>
  <br/>
  <h3>Important</h3>
  <ul>
    <li>The database upgrade can take several minutes.</li>
    <li>It is recommended to <b>back up database</b> before upgrading.</li>
    <li><b>Copy the directory /extensions</b> from previous version before upgrading.</li>
  </ul>
  <br/>
    <input  type="submit" value="Upgrade">
</div>
</form>
~~~

Automate and check with:

~~~
curl -s -o /dev/null --data "Upgrade=Upgrade" http://localhost:9000/sonar/setup/setup_database
sleep 30; test `curl -sL -w "%{http_code}" "http://localhost:9000/sonar/" -o /dev/null --max-redirs 0` != 200
~~~


### Known problems

* Sonar does not work on JDK8 (fails on 'configure widgets') something with Ruby

* Sonar update center overwrites our custom built plugins

--> disable in config.

* Plugins are bundled:

~~~
[root@lobot plugins]# ll /home/sonar/sonar/lib/bundled-plugins
total 6424
-rw-r--r-- 1 sonar sonar 4807365 Mar 29 18:26 sonar-findbugs-plugin-2.1.jar
-rw-r--r-- 1 sonar sonar  518221 Mar 29 18:26 sonar-jacoco-plugin-2.1.jar
-rw-r--r-- 1 sonar sonar   75122 Mar 29 18:26 sonar-java-plugin-2.1.jar
-rw-r--r-- 1 sonar sonar 1143689 Mar 29 18:26 sonar-squid-java-plugin-2.1.jar
-rw-r--r-- 1 sonar sonar   20857 Mar 29 18:26 sonar-surefire-plugin-2.1.jar
~~~

--> should be deleted.

* java.lang.NullPointerException

~~~
Caused by: java.lang.NullPointerException
    at org.apache.commons.io.IOUtils.copyLarge(IOUtils.java:1792)
    at org.apache.commons.io.IOUtils.copyLarge(IOUtils.java:1769)
    at org.apache.commons.io.IOUtils.copy(IOUtils.java:1744)
    at org.apache.commons.io.FileUtils.copyInputStreamToFile(FileUtils.java:1512)
~~~

--> you get this when a jar is missing in one of the sonar plugins, for example
    `annotations.jar` in the sonar-findbugs-plugin.
