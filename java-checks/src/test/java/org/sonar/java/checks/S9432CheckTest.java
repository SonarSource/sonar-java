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

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class S9432CheckTest {

  @Test
  void detects_blankness_checks_after_whitespace_removal() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/S9432CheckSample.java"))
      .withCheck(new S9432Check())
      .withJavaVersion(11)
      .verifyIssues();
  }

  @Test
  void ignores_pre_java_11_code() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/S9432CheckSample.java"))
      .withCheck(new S9432Check())
      .withJavaVersion(8)
      .verifyNoIssues();
  }

  @Test
  void ignores_code_without_semantic_information() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/S9432CheckSample.java"))
      .withCheck(new S9432Check())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
