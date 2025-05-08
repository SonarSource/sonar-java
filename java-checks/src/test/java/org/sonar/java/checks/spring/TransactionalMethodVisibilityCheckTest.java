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
package org.sonar.java.checks.spring;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class TransactionalMethodVisibilityCheckTest {

  @Test
  void test_Spring5() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/spring/TransactionalMethodVisibilityCheckSample_Spring5.java"))
      .withCheck(new TransactionalMethodVisibilityCheck())
      .verifyIssues();
  }

  @Test
  void test_spring6() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/spring/TransactionalMethodVisibilityCheckSample_Spring6.java"))
      .withCheck(new TransactionalMethodVisibilityCheck())
      .withClassPath(List.of(new File("spring-tx-6.0.1.jar"),
        new File("spring-context-6.0.1.jar")))
      .verifyIssues();
  }

  /** Check that when dependencies are not resolved, we do not raise issues. */
  @Test
  void test_noDependencies() {
    CheckVerifier.newVerifier()
      .onFiles(
        mainCodeSourcesPath("checks/spring/TransactionalMethodVisibilityCheckSample_Spring5.java"),
        mainCodeSourcesPath("checks/spring/TransactionalMethodVisibilityCheckSample_Spring6.java"))
      .withCheck(new TransactionalMethodVisibilityCheck())
      .withClassPath(Collections.emptyList())
      .verifyNoIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/spring/TransactionalMethodVisibilityCheckSample.java"))
      .withCheck(new TransactionalMethodVisibilityCheck())
      .verifyIssues();
  }
}
