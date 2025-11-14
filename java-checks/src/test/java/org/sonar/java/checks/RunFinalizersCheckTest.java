/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.model.JavaVersionImpl;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class RunFinalizersCheckTest {

  @Test
  void test() {
    int javaVersion = Optional.ofNullable(System.getProperty("java.specification.version"))
      .map(JavaVersionImpl::fromString)
      .orElse(new JavaVersionImpl())
      .asInt();
    if (javaVersion < 11) {
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/RunFinalizersCheckSample.java"))
        .withCheck(new RunFinalizersCheck())
        .verifyIssues();
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/RunFinalizersCheckSample.java"))
        .withCheck(new RunFinalizersCheck())
        .withJavaVersion(javaVersion)
        .verifyIssues();
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/RunFinalizersCheckNoIssue.java"))
        .withCheck(new RunFinalizersCheck())
        .withJavaVersion(11)
        .verifyNoIssues();
    } else {
      // No issue raised starting JDK 11 as the related APIs were removed from JDK and cannot be resolved
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/RunFinalizersCheckNoIssue.java"))
        .withCheck(new RunFinalizersCheck())
        .verifyNoIssues();
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/RunFinalizersCheckNoIssue.java"))
        .withCheck(new RunFinalizersCheck())
        .withJavaVersion(javaVersion)
        .verifyNoIssues();
      CheckVerifier.newVerifier()
        .onFile(nonCompilingTestSourcesPath("checks/RunFinalizersCheckNoIssue.java"))
        .withCheck(new RunFinalizersCheck())
        .withJavaVersion(10)
        .verifyNoIssues();
    }
  }
}
