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

import org.junit.jupiter.api.Test;
import org.sonar.java.AnalysisException;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class CommentRegularExpressionCheckTest {

  @Test
  void test() {
    CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
    check.regularExpression = "(?i).*TODO.*";
    check.message = "Avoid TODO";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/CommentRegularExpressionCheck.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void should_not_fail_with_empty_regular_expression() {
    CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
    check.regularExpression = "";
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/CommentRegularExpressionCheck2.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void bad_regex() {
    CommentRegularExpressionCheck check = new CommentRegularExpressionCheck();
    check.regularExpression = "[[";
    CheckVerifier verifier = CheckVerifier.newVerifier().onFile("src/test/files/checks/CommentRegularExpressionCheck.java").withCheck(check);
    assertThrows(AnalysisException.class, verifier::verifyIssues);
  }

}
