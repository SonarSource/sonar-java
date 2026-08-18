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

class BigDecimalEqualsCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BigDecimalEqualsCheckSample.java"))
      .withCheck(new BigDecimalEqualsCheck())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BigDecimalEqualsCheckSample.java"))
      .withCheck(new BigDecimalEqualsCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void test_guava() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BigDecimalEqualsCheckGuavaSample.java"))
      .withCheck(new BigDecimalEqualsCheck())
      .verifyIssues();
  }

  @Test
  void test_guava_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/BigDecimalEqualsCheckGuavaSample.java"))
      .withCheck(new BigDecimalEqualsCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
