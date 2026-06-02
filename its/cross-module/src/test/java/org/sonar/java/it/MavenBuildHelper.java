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
package org.sonar.java.it;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class MavenBuildHelper {

  static final String SOURCES_DIR = "src/main/java";
  static final String BINARIES_DIR = "target/classes";

  private final Path projectBaseDir;

  MavenBuildHelper(Path projectBaseDir) {
    this.projectBaseDir = projectBaseDir;
  }

  void build() {
    runMaven("clean", "compile", "dependency:build-classpath", "-Dmdep.outputFile=target/classpath.txt");
  }

  String resolveLibraries(String moduleName, Set<String> allModuleNames) {
    var libraries = new ArrayList<String>();

    Path classpathFile = projectBaseDir.resolve(moduleName).resolve("target/classpath.txt");
    if (Files.exists(classpathFile)) {
      try {
        String classpath = Files.readString(classpathFile).trim();
        if (!classpath.isEmpty()) {
          libraries.addAll(Arrays.asList(classpath.split(File.pathSeparator)));
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to read classpath file for module: " + moduleName, e);
      }
    }

    // Add sibling modules' compiled classes for cross-module resolution
    for (String otherModule : allModuleNames) {
      if (!otherModule.equals(moduleName)) {
        Path otherClasses = projectBaseDir.resolve(otherModule).resolve("target/classes");
        if (Files.isDirectory(otherClasses)) {
          libraries.add(otherClasses.toString());
        }
      }
    }

    return String.join(",", libraries);
  }

  private void runMaven(String... goals) {
    var command = new ArrayList<>(List.of("mvn", "-B", "-q"));
    command.addAll(List.of(goals));
    try {
      var process = new ProcessBuilder(command)
        .directory(projectBaseDir.toFile())
        .redirectErrorStream(true)
        .start();
      String output = new String(process.getInputStream().readAllBytes());
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new AssertionError("Maven build failed with exit code " + exitCode + ":\n" + output);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to run Maven build", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Maven build was interrupted", e);
    }
  }
}
