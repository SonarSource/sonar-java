/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class OptionalGetBeforeIsPresentCheckTest {

  @Test
  void test_with_jdk_8() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/OptionalGetBeforeIsPresentCheckSample.java"))
      .withCheck(new OptionalGetBeforeIsPresentCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void test_with_jdk_11() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/OptionalGetBeforeIsPresentCheck_jdk11.java"))
      .withCheck(new OptionalGetBeforeIsPresentCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .withJavaVersion(11)
      .verifyIssues();
  }

  @Test
  void test_ofNullable() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/OptionalGetBeforeIsPresentCheck_ofNullable.java"))
      .withChecks(new OptionalGetBeforeIsPresentCheck(), new NullDereferenceCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .withJavaVersion(11)
      .verifyNoIssues();
  }

  @Test
  void invocation_leading_to_NoSuchElementException() {
    SECheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("symbolicexecution/checks/MethodInvocationLeadingToNSEE.java"))
      .withCheck(new OptionalGetBeforeIsPresentCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .withJavaVersion(8)
      .verifyIssues();
  }

}
