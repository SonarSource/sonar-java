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
package org.sonar.java.se.checks;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

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
  void test_deferred_reporting() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NPE_deferred.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test_npe_transitive() {
    SECheckVerifier.newVerifier()
      .onFile("src/test/files/se/NPE_transitive.java")
      .withCheck(new NullDereferenceCheck())
      .verifyIssues();
  }

  @Test
  void test_booleanValue_method() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/NullFromBooleanValueCall.java"))
      .withChecks(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void test_optional_of_nullable_jspecify() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/OptionalOfNullableCall_jspecify.java"))
      .withChecks(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void test_optional_of_nullable_javax() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/OptionalOfNullableCall_javax.java"))
      .withChecks(new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }
}
