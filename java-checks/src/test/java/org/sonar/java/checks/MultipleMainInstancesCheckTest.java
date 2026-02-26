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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class MultipleMainInstancesCheckTest {
  private static final List<String> TEST_SOURCES = List.of(
    mainCodeSourcesPath("checks/MultipleMainInstancesSample.java"),
    nonCompilingTestSourcesPath("checks/MultipleMainInstancesSample.java")
  );

  @Test
  void test() {
    TEST_SOURCES.forEach(file ->
      CheckVerifier.newVerifier()
        .onFile(file)
        .withCheck(new MultipleMainInstancesCheck())
        .withJavaVersion(25)
        .verifyIssues()
    );
  }

  @Test
  void test_java_24() {
    TEST_SOURCES.forEach(file ->
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/MultipleMainInstancesSample.java"))
        .withCheck(new MultipleMainInstancesCheck())
        .withJavaVersion(24)
        .verifyNoIssues()
    );
  }
}
