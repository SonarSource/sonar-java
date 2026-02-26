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
package org.sonar.java.checks;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class MultipleMainInstancesCheckTest {

  static Stream<String> testSamples() {
    return Stream.of(
      mainCodeSourcesPath("checks/MultipleMainInstancesSample.java"),
      nonCompilingTestSourcesPath("checks/MultipleMainInstancesNonCompilingSample.java")
    );
  }

  @ParameterizedTest
  @MethodSource("testSamples")
  void test(String file) {
    CheckVerifier.newVerifier()
      .onFile(file)
      .withCheck(new MultipleMainInstancesCheck())
      .withJavaVersion(25)
      .verifyIssues();
  }

  @ParameterizedTest
  @MethodSource("testSamples")
  void test_java_24(String file) {
    CheckVerifier.newVerifier()
      .onFile(file)
      .withCheck(new MultipleMainInstancesCheck())
      .withJavaVersion(24)
      .verifyNoIssues();
  }
}
