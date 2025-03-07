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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DependencyVersionImplTest {

  @Test
  void versionComparisons() {
    DependencyVersionImpl dependencyVersion = new DependencyVersionImpl("org.example", "example-artifact",
      Version.parse("3.2.6-rc1").get());

    assertTrue(dependencyVersion.isGreaterThanOrEqualTo("3.2"));
    assertTrue(dependencyVersion.isGreaterThanOrEqualTo("3.2.5"));
    assertTrue(dependencyVersion.isGreaterThanOrEqualTo("2.9.4-rc2"));
    assertFalse(dependencyVersion.isGreaterThanOrEqualTo("3.2.11"));
    assertTrue(dependencyVersion.isLowerThan("4.0.0.RELEASE"));

    assertTrue(dependencyVersion.isLowerThan("3.3.0"));
    assertFalse(dependencyVersion.isLowerThan("3.2.6-rc1"));

    assertTrue(dependencyVersion.isGreaterThan("3.0.77"));
    assertFalse(dependencyVersion.isGreaterThan("3.2"));
    assertFalse(dependencyVersion.isGreaterThan("3.2.6-rc1"));

    assertTrue(dependencyVersion.isLowerThanOrEqualTo("3.2.6-rc1"));
    assertFalse(dependencyVersion.isLowerThanOrEqualTo("3.2.5"));
  }
}
