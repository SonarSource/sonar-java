/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.sonar.java.CheckTestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.CheckTestUtils.testSourcesPath;

class NoTestInTestClassCheckTest {

  @Test
  void test() {
    JavaCheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/NoTestInTestClassCheck.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/NoTestInTestClassCheck.java"))
      .withCheck(new NoTestInTestClassCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void surefire_inclusions_class_name_pattern() {
    NoTestInTestClassCheck check = new NoTestInTestClassCheck();
    check.testClassNamePattern = "Test.*|.*(Test|Tests|TestCase)";
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/NoTestInTestClassCustomPattern.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void empty_class_name_pattern() {
    NoTestInTestClassCheck check = new NoTestInTestClassCheck();
    check.testClassNamePattern = "";
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/NoTestInTestClassCustomPattern.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testEnclosed() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/NoTestInTestClassCheckEnclosed.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/NoTestInTestClassCheckEnclosed.java"))
      .withCheck(new NoTestInTestClassCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void noClasspath() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/NoTestInTestClassCheckNoClasspath.java"))
      .withCheck(new NoTestInTestClassCheck())
      .withClassPath(Collections.emptyList())
      .verifyIssues();
  }

  @Test
  void archUnit() {
    JavaCheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/NoTestInTestClassCheckArchUnitTest.java"))
      .withCheck(new NoTestInTestClassCheck())
      .verifyIssues();
  }

}
