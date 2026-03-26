/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class FlexibleConstructorBodyValidationCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/FlexibleConstructorBodyValidationCheckSample.java"))
      .withCheck(new FlexibleConstructorBodyValidationCheck())
      .withJavaVersion(25)
      .verifyIssues();
  }

  @Test
  void test_java_24() {
    // Should not raise issues on Java 24 and below (feature not available)
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/FlexibleConstructorBodyValidationCheckSample.java"))
      .withCheck(new FlexibleConstructorBodyValidationCheck())
      .withJavaVersion(24)
      .verifyNoIssues();
  }
}
