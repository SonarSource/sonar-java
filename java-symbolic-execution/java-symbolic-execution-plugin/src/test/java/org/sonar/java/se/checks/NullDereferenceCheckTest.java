/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se.checks;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class NullDereferenceCheckTest {

  @Test
  void test_unboxing() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/PrimitiveUnboxing.java"))
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test_unboxing_without_binaries() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/PrimitiveUnboxing.java"))
      .withClassPath(Collections.emptyList())
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test_constants_in_loop() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/NullDereferenceCheck_constants_in_loop.java"))
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NullDereferenceCheck.java")
      .withCheck(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void objectsMethodsTest() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/ObjectsMethodsTest.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void null_array_access() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NullArrayAccess.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void chained_method_invocation_issue_order() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/MethodParamInvocationOrder.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void invocation_leading_to_NPE() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/MethodInvocationLeadingToNPE.java")
      .withCheck(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void reporting_test() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NPE_reporting.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void ruling() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NPEwithZeroTests.java")
      .withCheck(new NullDereferenceCheck())
      .verifyNoIssues();
  }

  @Test
  void test_deferred_reporting() throws Exception {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NPE_deferred.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test_npe_transitive() throws Exception {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NPE_transitive.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test_booleanValue_method() throws Exception {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/NullFromBooleanValueCall.java"))
      .withChecks(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }
}
