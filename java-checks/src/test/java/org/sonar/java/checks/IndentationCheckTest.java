/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class IndentationCheckTest {

  @Test
  void detected_default_indentation_level() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/IndentationCheck_default.java"))
      .withCheck(new IndentationCheck())
      .verifyIssues();
  }

  @Test
  void detected_custom_level() {
    IndentationCheck check = new IndentationCheck();
    check.indentationLevel = 4;
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IndentationCheck_custom.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void assume_tab_is_indentation_level() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IndentationCheck_tab.java"))
      .withCheck(new IndentationCheck())
      .verifyIssues();
  }

  @Test
  void tolerates_line_breaking_control_characters() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IndentationCheckWithControlCharacters.java"))
      .withCheck(new IndentationCheck())
      .verifyNoIssues();
  }

  @Test
  void compact_source_file() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IndentationCheck_compactSource.java"))
      .withCheck(new IndentationCheck())
      .verifyIssues();
  }
}
