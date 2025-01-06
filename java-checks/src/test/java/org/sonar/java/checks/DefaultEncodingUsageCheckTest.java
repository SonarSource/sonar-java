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
package org.sonar.java.checks;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class DefaultEncodingUsageCheckTest {

  @ParameterizedTest
  @ValueSource(ints = {8, 11, 17})
  void test_before_java_version_18(int javaVersion) {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DefaultEncodingUsageCheckSample.java"))
      .withCheck(new DefaultEncodingUsageCheck())
      .withJavaVersion(javaVersion)
      .verifyIssues();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 18, 19})
  void test_since_java_version_18_or_not_set(int javaVersion) {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DefaultEncodingUsageCheckSample.java"))
      .withCheck(new DefaultEncodingUsageCheck())
      .withJavaVersion(javaVersion)
      .verifyNoIssues();
  }

}
