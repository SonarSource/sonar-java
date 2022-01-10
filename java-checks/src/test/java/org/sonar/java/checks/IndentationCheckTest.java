/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class IndentationCheckTest {

  @Test
  void detected_default_indentation_level() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/IndentationCheck_default.java")
      .withCheck(new IndentationCheck())
      .verifyIssues();
  }

  @Test
  void detected_custom_level() {
    IndentationCheck check = new IndentationCheck();
    check.indentationLevel = 4;
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/IndentationCheck_custom.java")
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void assume_tab_is_indentation_level() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/IndentationCheck_tab.java"))
      .withCheck(new IndentationCheck())
      .verifyIssues();
  }

  @Test
  void tolerates_line_breaking_control_characters() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/IndentationCheckWithControlCharacters.java"))
      .withCheck(new IndentationCheck())
      .verifyNoIssues();
  }
}
