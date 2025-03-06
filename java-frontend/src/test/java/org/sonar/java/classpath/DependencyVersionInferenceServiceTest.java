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
import org.junit.jupiter.api.Test;
import org.sonar.java.test.classpath.TestClasspathUtils;
import org.sonar.plugins.java.api.classpath.DependencyVersion;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DependencyVersionInferenceServiceTest {

  @Test
  void inferAllSpring() {
    var springClasspath = TestClasspathUtils
      .loadFromFile("../java-checks-test-sources/spring-3.2/target/test-classpath.txt");

    List<DependencyVersion> dependencyVersions = DependencyVersionInferenceService.make().inferAll(springClasspath);

    assertThat(dependencyVersions.size()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void inferAllDefault() {
    var springClasspath = TestClasspathUtils
      .loadFromFile("../java-checks-test-sources/default/target/test-classpath.txt");

    List<DependencyVersion> dependencyVersions = DependencyVersionInferenceService.make().inferAll(springClasspath);

    assertThat(dependencyVersions.size()).isGreaterThanOrEqualTo(3);
  }
}
