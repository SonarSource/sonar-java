/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.test.classpath.TestClasspathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class AssertionsWithoutMessageCheckTest {

  @Test
  void test_Testng77() {
    List<File> classPath = new ArrayList<>(TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsWithoutMessageCheckSample.java"))
      .withCheck(new AssertionsWithoutMessageCheck())
      .withClassPath(classPath)
      .verifyIssues();
  }

  @Test
  void test_Testng75() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsWithoutMessageCheckSample_Testng75.java"))
      .withCheck(new AssertionsWithoutMessageCheck())
      .addJarsToClasspath("testng-7.5.1")
      .removeJarsFromClasspath("testng-7.12.0")
      .verifyNoIssues();
  }
}
