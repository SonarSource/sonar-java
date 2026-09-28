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
import java.util.concurrent.TimeUnit;

class MavenBuildHelper {

  static final String SOURCES_DIR = "src/main/java";
  static final String BINARIES_DIR = "target/classes";

  private static final long BUILD_TIMEOUT_SECONDS = 45;

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

  private static final String MAVEN_EXECUTABLE =
    System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";

  private void runMaven(String... goals) {
    var command = new ArrayList<>(List.of(MAVEN_EXECUTABLE, "-B", "-q"));
    command.addAll(List.of(goals));
    Process process = null;
    try {
      process = new ProcessBuilder(command)
        .directory(projectBaseDir.toFile())
        .redirectErrorStream(true)
        .start();
      // Drain the output stream in a separate thread so the process is not blocked
      // by a full output buffer while we are waiting on the timeout.
      var outputCapture = new StringBuilder();
      Process finalProcess = process;
      var reader = new Thread(() -> {
        try {
          outputCapture.append(new String(finalProcess.getInputStream().readAllBytes()));
        } catch (IOException e) {
          // process output is no longer readable (e.g. it was destroyed); ignore
        }
      });
      reader.setDaemon(true);
      reader.start();

      if (!process.waitFor(BUILD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        throw new AssertionError("Maven build timed out after " + BUILD_TIMEOUT_SECONDS + " seconds");
      }
      reader.join(TimeUnit.SECONDS.toMillis(5));

      int exitCode = process.exitValue();
      if (exitCode != 0) {
        throw new AssertionError("Maven build failed with exit code " + exitCode + ":\n" + outputCapture);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to run Maven build", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Maven build was interrupted", e);
    } finally {
      if (process != null && process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }
}
