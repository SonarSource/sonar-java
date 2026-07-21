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
package org.sonar.java.checks.quarkus;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class SingletonInsteadOfApplicationScopedCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/quarkus/SingletonInsteadOfApplicationScopedCheckSample.java"))
      .withCheck(new SingletonInsteadOfApplicationScopedCheck())
      .verifyIssues();
  }

  @Test
  void testWithoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/quarkus/SingletonInsteadOfApplicationScopedCheckSample.java"))
      .withCheck(new SingletonInsteadOfApplicationScopedCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testNonQuarkusFile() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/quarkus/SingletonInsteadOfApplicationScopedCheckSampleNonQuarkus.java"))
      .withCheck(new SingletonInsteadOfApplicationScopedCheck())
      .verifyNoIssues();
  }
}
