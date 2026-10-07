/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package com.sonar.it.java.suite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class ReactorProject {

  private static final String GROUP = "org.sonarsource.it";
  private static final String ARTIFACT = "source-only-comparison";
  private static final String VERSION = "1.0";

  private ReactorProject() {
  }

  static void write(Path project, RepositoryScope scope, Map<String, List<Path>> moduleLibraries, String scannerVersion) throws IOException {
    if (!moduleLibraries.isEmpty() && !moduleLibraries.keySet().equals(Set.copyOf(scope.modulePaths()))) {
      throw new IllegalArgumentException("External classpaths must cover every scoped module or be empty for all modules");
    }
    Path root = project.toAbsolutePath().normalize();
    for (RepositoryScope.Module module : scope.modules()) {
      Path directory = root.resolve(module.path()).normalize();
      if (!directory.startsWith(root) || directory.equals(root)) {
        throw new IllegalArgumentException("Scanner module must stay below the copied project root: " + module.path());
      }
      List<Path> libraries = AnalysisDataset.validateLibraries(moduleLibraries.getOrDefault(module.path(), List.of()));
      if (!libraries.equals(AnalysisDataset.externalLibraries(libraries, scope.projectArtifactIds()))) {
        throw new IllegalArgumentException("Compiled target-module artifacts must not be supplied to " + module.path());
      }
      Files.createDirectories(directory);
      String relativeParent = directory.relativize(root.resolve("pom.xml")).toString().replace('\\', '/');
      String classpath = libraries.stream().map(Path::toString).collect(Collectors.joining(","));
      Files.writeString(directory.resolve("pom.xml"), """
        <project xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion>
          <parent>
            <groupId>%s</groupId>
            <artifactId>%s</artifactId>
            <version>%s</version>
            <relativePath>%s</relativePath>
          </parent>
          <artifactId>%s</artifactId>
          <properties>
            <sonar.sources>src/main/java</sonar.sources>
            <sonar.tests></sonar.tests>
            <sonar.java.libraries>%s</sonar.java.libraries>
            <sonar.java.binaries></sonar.java.binaries>
            <sonar.java.test.binaries></sonar.java.test.binaries>
            <sonar.java.test.libraries></sonar.java.test.libraries>
          </properties>
        </project>
        """.formatted(GROUP, ARTIFACT, VERSION, xml(relativeParent), xml(module.artifactId()), xml(classpath)));
    }
    String modules = scope.modulePaths().stream().map(path -> "    <module>" + xml(path) + "</module>").collect(Collectors.joining("\n"));
    Files.createDirectories(root);
    Files.writeString(root.resolve("pom.xml"), """
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>%s</groupId>
        <artifactId>%s</artifactId>
        <version>%s</version>
        <packaging>pom</packaging>
        <properties>
          <maven.compiler.release>21</maven.compiler.release>
          <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
          <sonar.sources></sonar.sources>
          <sonar.tests></sonar.tests>
          <sonar.java.libraries></sonar.java.libraries>
          <sonar.java.binaries></sonar.java.binaries>
          <sonar.java.test.binaries></sonar.java.test.binaries>
          <sonar.java.test.libraries></sonar.java.test.libraries>
        </properties>
        <modules>
      %s
        </modules>
        <build>
          <plugins>
            <plugin>
              <groupId>org.sonarsource.scanner.maven</groupId>
              <artifactId>sonar-maven-plugin</artifactId>
              <version>%s</version>
            </plugin>
          </plugins>
        </build>
      </project>
      """.formatted(GROUP, ARTIFACT, VERSION, modules, xml(scannerVersion)));
  }

  private static String xml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
  }
}
