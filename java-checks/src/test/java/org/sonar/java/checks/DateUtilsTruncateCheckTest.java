/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class DateUtilsTruncateCheckTest {

  private static final String FILENAME = mainCodeSourcesPath("checks/DateUtilsTruncateCheckSample.java");

  @Test
  void java8() {
    CheckVerifier.newVerifier()
      .onFile(FILENAME)
      .withCheck(new DateUtilsTruncateCheck())
      .withJavaVersion(8)
      .verifyIssues();
  }

  @Test
  void java7() {
    CheckVerifier.newVerifier()
      .onFile(FILENAME)
      .withCheck(new DateUtilsTruncateCheck())
      .withJavaVersion(7)
      .verifyNoIssues();
  }

  @Test
  void unknown_version() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/DateUtilsTruncateCheck_no_version.java"))
      .withCheck(new DateUtilsTruncateCheck())
      .verifyIssues();
  }
}
