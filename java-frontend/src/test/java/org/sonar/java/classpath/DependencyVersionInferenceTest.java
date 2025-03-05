/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.classpath;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.sonar.java.test.classpath.TestClasspathUtils;

class DependencyVersionInferenceTest {
  List<File> classpath = TestClasspathUtils
    .loadFromFile("../java-checks-test-sources/spring-3.2/target/test-classpath.txt");
  List<File> lombokClasspath = TestClasspathUtils
    .loadFromFile("../java-checks-test-sources/default/target/test-classpath.txt");

  @Test
  void inferByName() {
    // Arrange
    DependencyVersionInference lombokInference = new DependencyVersionInference.ByNameInference(DependencyVersionInference.LOMBOK_PATTERN, "org.projectlombok", "lombok");

    // Act
    Optional<Version> version = lombokInference.infer(lombokClasspath);

    // Assert
    assertEquals(1, version.get().major());
    assertEquals(18, version.get().minor());
    assertEquals(30, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }

  @Test
  @Disabled("this method is not working for lombok")
  void inferByReflection() {
    // Arrange
    DependencyVersionInference lombokInference = new DependencyVersionInference.ReflectiveInference();

    // Act
    Optional<Version> version = lombokInference.infer(lombokClasspath);

    // Assert
    assertEquals(1, version.get().major());
    assertEquals(18, version.get().minor());
    assertEquals(30, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }

  @Test
  void inferByManifest() {
    // Arrange
    DependencyVersionInference lombokInference =
      new DependencyVersionInference.ManifestInference("Lombok-Version", "org.projectlombok", "lombok");

    // Act
    Optional<Version> version = lombokInference.infer(lombokClasspath);

    // Assert
    assertEquals(1, version.get().major());
    assertEquals(18, version.get().minor());
    assertEquals(30, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }

  @Test
  void inferenceImplementations() {
    // Act
    Optional<Version> version =
      DependencyVersionInference.inferenceImplementations.stream()
        .filter(i -> i.handles("org.springframework.boot", "spring-boot"))
        .filter(i -> i instanceof DependencyVersionInference.ManifestInference)
        .map(i -> i.infer(classpath))
        .flatMap(Optional::stream)
        .findFirst();

    // Assert
    assertEquals(3, version.get().major());
    assertEquals(2, version.get().minor());
    assertEquals(4, version.get().patch());
    assertEquals(null, version.get().qualifier());
  }
}
