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
package org.sonar.java.checks.tests;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class NoTestInTestClassCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/NoTestInTestClassCheckSample.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

  @Test
  void surefire_inclusions_class_name_pattern() {
    NoTestInTestClassCheck check = new NoTestInTestClassCheck();
    check.testClassNamePattern = "Test.*|.*(Test|Tests|TestCase)";
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCustomPattern.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void empty_class_name_pattern() {
    NoTestInTestClassCheck check = new NoTestInTestClassCheck();
    check.testClassNamePattern = "";
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCustomPattern.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testEnclosed() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCheckEnclosed.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

  @Test
  void noClasspath() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCheckNoClasspath.java"))
      .withCheck(new NoTestInTestClassCheck())
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }

  @Test
  void archUnit() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCheckArchUnitTest.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

  @Test
  void pactUnit() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestsInTestClassCheckPactTest.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

  @Test
  void testNg() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCheckTestNgTest.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

  @Test
  void testCucumber() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCheckCucumberTest.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

  @Test
  void testCucumberWithoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestInTestClassCheckCucumberWithoutSemanticTest.java"))
      .withCheck(new NoTestInTestClassCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testExtensionWithoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/tests/NoTestInTestClassExtension.java"))
      .withCheck(new NoTestInTestClassCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
