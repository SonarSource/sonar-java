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
package org.sonar.java.sanity;


import org.junit.jupiter.api.Test;
import org.sonar.java.checks.AnnotationDefaultArgumentCheck;
import org.sonar.java.checks.AssignmentInSubExpressionCheck;
import org.sonar.java.checks.ParameterReassignedToCheck;
import org.sonar.java.checks.RedundantRecordMethodsCheck;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

/**
 * The goal of these tests is to check that setting sonar.ignoreUnnamedSplitPackage to true does not make
 * this combination of checks and test sample fail
 */
class IgnoreUnnamedSplitPackageTest {
  @Test
  void test_individual() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/NoTestInTestClassCheck.java"))
      .withCheck(new AnnotationDefaultArgumentCheck())
      .verifyNoIssues();

    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/NoTestsInTestClassCheckPactTest.java"))
      .withCheck(new AnnotationDefaultArgumentCheck())
      .verifyNoIssues();

    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/RestrictedIdentifiersUsageCheck.java"))
      .withJavaVersion(11)
      .withCheck(new AssignmentInSubExpressionCheck())
      .verifyNoIssues();

    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/RestrictedIdentifiersUsageCheck.java"))
      .withCheck(new ParameterReassignedToCheck())
      .withJavaVersion(11)
      .verifyNoIssues();


    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/AccessibilityChangeCheck.java"))
      .withCheck(new RedundantRecordMethodsCheck())
      .verifyNoIssues();

    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/serialization/SerialVersionUidInRecordCheck.java"))
      .withCheck(new RedundantRecordMethodsCheck())
      .verifyNoIssues();
  }

  @Test
  void test_combination() {
    CheckVerifier.newVerifier()
      .onFiles(
        nonCompilingTestSourcesPath("checks/NoTestInTestClassCheck.java"),
        nonCompilingTestSourcesPath("checks/RestrictedIdentifiersUsageCheck.java"),
        nonCompilingTestSourcesPath("checks/RestrictedIdentifiersUsageCheck.java"),
        nonCompilingTestSourcesPath("checks/AccessibilityChangeCheck.java")
        //, nonCompilingTestSourcesPath("checks/serialization/SerialVersionUidInRecordCheck.java")
      )
      .withChecks(
        new AnnotationDefaultArgumentCheck(),
        new AssignmentInSubExpressionCheck(),
        new ParameterReassignedToCheck(),
        new RedundantRecordMethodsCheck()
      )
      .withJavaVersion(11)
      .verifyNoIssues();

    CheckVerifier.newVerifier()
      .onFiles(
        nonCompilingTestSourcesPath("checks/NoTestInTestClassCheck.java"),
        nonCompilingTestSourcesPath("checks/AccessibilityChangeCheck.java"),
        nonCompilingTestSourcesPath("checks/serialization/SerialVersionUidInRecordCheck.java")
      )
      .withChecks(
        new AnnotationDefaultArgumentCheck(),
        new AssignmentInSubExpressionCheck(),
        new ParameterReassignedToCheck(),
        new RedundantRecordMethodsCheck()
      )
      .withJavaVersion(17)
      .verifyNoIssues();

  }
}
