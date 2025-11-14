/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DependencyVersionInferenceTest {

  @Test
  void inferLombok() {
    // Arrange
    List<File> lombokClasspath = TestClasspathUtils
      .loadFromFile("../java-checks-test-sources/default/target/test-classpath.txt");

    // Act
    Optional<Version> version = new DependencyVersionInference().infer("lombok", lombokClasspath);

    // Assert
    Assertions.assertTrue(version.isPresent());
    assertEquals(new VersionImpl(1, 18, 38, null), version.get());
  }


  @Test
  void inferenceSpringBoot() {
    // Arrange
    List<File> classpath = TestClasspathUtils
      .loadFromFile("../java-checks-test-sources/spring-3.2/target/test-classpath.txt");

    // Act
    Optional<Version> version =
      new DependencyVersionInference().infer("spring-boot", classpath);

    // Assert
    Assertions.assertTrue(version.isPresent());
    assertEquals(new VersionImpl(3, 2, 4, null), version.get());
  }
}
