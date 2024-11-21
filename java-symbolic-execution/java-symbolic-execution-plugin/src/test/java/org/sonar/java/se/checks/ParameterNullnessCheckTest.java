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
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.se.SECheckVerifier;
import org.sonar.java.se.utils.SETestUtils;

class ParameterNullnessCheckTest {

  @Test
  void test() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/ParameterNullnessCheck/noDefault/ParameterNullnessCheckSample.java"))
      .withCheck(new ParameterNullnessCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_package_annotation() {
    SECheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("symbolicexecution/checks/ParameterNullnessCheck/packageNonNull/ParameterNullnessCheckSample.java"))
      .withCheck(new ParameterNullnessCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

}
