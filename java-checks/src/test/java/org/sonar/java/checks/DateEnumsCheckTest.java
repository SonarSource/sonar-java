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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class DateEnumsCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DateEnumsCheckSample.java"))
      .withCheck(new DateEnumsCheck())
      .verifyIssues();
  }

  @Test
  void test_quickfix_import() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DateEnumsCheckImportSample.java"))
      .withCheck(new DateEnumsCheck())
      .verifyIssues();
  }

  @Test
  void test_above_threshold() {
    // 8 out of 9 total usages use int literals (89%) -> above 80% threshold -> considered code-style -> no issues raised
    CheckVerifier.newVerifier()
      .onFiles(List.of(
        mainCodeSourcesPath("checks/s8694/above/IntLiteralFile.java"),
        mainCodeSourcesPath("checks/s8694/above/EnumFile.java")))
      .withCheck(new DateEnumsCheck())
      .verifyNoIssues();
  }

  @Test
  void test_below_threshold() {
    // 3 out of 7 total usages use int literals (43%) -> below 80% threshold -> issues are raised
    CheckVerifier.newVerifier()
      .onFiles(List.of(
        mainCodeSourcesPath("checks/s8694/below/IntLiteralFile.java"),
        mainCodeSourcesPath("checks/s8694/below/EnumFile.java")))
      .withCheck(new DateEnumsCheck())
      .verifyIssues();
  }
}
