/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.test.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TestClasspathUtils {

  private TestClasspathUtils() {
    // utility class
  }

  public static List<File> loadFromFile(String classpathTextFilePath) {
    List<File> classpath = new ArrayList<>();
    String mavenRepository = findMavenLocalRepository(System::getenv, System::getProperty);
    try {
      String content = Files.readString(Paths.get(classpathTextFilePath.replace('/', File.separatorChar)), UTF_8);
      Arrays.stream(content.split(":"))
        .map(String::trim)
        .filter(line -> !line.isBlank())
        .map(line -> line.replace('/', File.separatorChar))
        .map(line -> line.replace("${M2_REPO}", mavenRepository))
        .map(Paths::get)
        .forEach(dependencyPath ->{
          if (!Files.exists(dependencyPath)) {
            throw new IllegalArgumentException("Missing dependency: " + dependencyPath);
          }
          classpath.add(dependencyPath.toFile());
        });
    } catch (IOException e) {
      throw new IllegalArgumentException("Exception while loading '" + classpathTextFilePath + "': " + e.getMessage(), e);
    }
    return classpath;
  }

  // VisibleForTesting
  static String findMavenLocalRepository(UnaryOperator<String> systemEnvProvider, UnaryOperator<String> systemPropertyProvider) {
    String repository = systemEnvProvider.apply("M2_REPO");
    if (repository == null || repository.isEmpty()) {
      // In the root pom.xml file, the surefire plugin's configuration always set the M2_REPO env variable to the right directory.
      // Here we default to ~/.m2/repository only for IDE execution that doesn't use the maven surefire plugin configuration.
      repository = Path.of(systemPropertyProvider.apply("user.home")).resolve(".m2").resolve("repository").toString();
    }
    return repository;
  }

}
