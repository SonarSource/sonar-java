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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class TooManyLinesOfCodeInFileCheckTest {

  @Test
  void testDefault() {
    assertThat(new TooManyLinesOfCodeInFileCheck().maximum).isEqualTo(750);
  }

  @Test
  void test() {
    TooManyLinesOfCodeInFileCheck check = new TooManyLinesOfCodeInFileCheck();
    check.maximum = 1;
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/TooManyLinesOfCode.java"))
      .withCheck(check)
      .verifyIssueOnFile("This file has 11 lines, which is greater than 1 authorized. Split it into smaller files.");
  }

  @Test
  void test2() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/TooManyLinesOfCode.java"))
      .withCheck(new TooManyLinesOfCodeInFileCheck())
      .verifyNoIssues();
  }

}
