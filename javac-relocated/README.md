# Relocated OpenJDK javac

The [module POM](pom.xml) pins the OpenJDK tag, commit, and source archive URL. During Maven's `generate-sources` phase, [RelocateJavac.java](src/generator/java/RelocateJavac.java) downloads that archive and generates `src/main/java/org/sonar/java/javac`. The generated directory is ignored by Git. `mvn -pl javac-relocated package` works from a fresh SonarJava checkout with Java 21 and network access; no OpenJDK build is needed. The generated compiler can parse Java 27 source on Java 21, and it can resolve Java 27 platform symbols when invoked with `--system` pointing to a JDK 27 image.

The generator compiles and runs OpenJDK's own `CompileProperties`, `PropertiesParser`, and `FlagsGenerator` tools from the archive. They produce the compiler's generated resource and flag classes without running `prepare.sh` or `make images`. Its `version.txt` records the Maven properties and [annotation proxy support source](src/generator/resources/AnnotationProxySupport.java) used for generation. A matching version reuses the directory; changed inputs replace it.

A shallow checkout of the same tag works as a local input:

```sh
git clone --depth 1 --branch 'jdk-27+35' https://github.com/openjdk/jdk.git /tmp/openjdk-jdk-27
java javac-relocated/src/generator/java/RelocateJavac.java --local /tmp/openjdk-jdk-27 'jdk-27+35' 815ff4dc327fe17f2433c7d115a5a503af10f3c4
```

Maven normally uses the pinned [official OpenJDK archive](https://github.com/openjdk/jdk/archive/815ff4dc327fe17f2433c7d115a5a503af10f3c4.zip). The generated sources retain OpenJDK's headers and license, and the module's POM disables SonarSource's source header checker for these files.
