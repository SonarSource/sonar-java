/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AvoidBrandInMethodNamesRuleTest {

  // Register the logTester, to see the Syntax Tree when running tests and executing the rule
  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void detected() {
    // Verifies that the check will raise the adequate issues with the expected message.
    // In the test file, lines which should raise an issue have been commented out
    // by using the following syntax: "// Noncompliant {{EXPECTED_MESSAGE}}"
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidBrandInMethodNamesRule.java")
      .withCheck(new AvoidBrandInMethodNamesRule())
      .verifyIssues();
  }

  @Test
  void printingTreeInDebug() {
    // Make sure logs are visible during the tests, and at DEBUG level in production
    logTester.setLevel(LoggerLevel.DEBUG);

    // re-execute the test
    JavaCheckVerifier.newVerifier()
    .onFile("src/test/files/AvoidBrandInMethodNamesRule.java")
    .withCheck(new AvoidBrandInMethodNamesRule())
    .verifyIssues();

    // verify that the tree has been correctly displayed in DEBUG
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .hasSize(1)
      .containsOnly("CompilationUnitTree : [\n"
        + "  ClassTree\n"
        + "    ModifiersTree\n"
        + "    IdentifierTree\n"
        + "    TypeParameters : [\n"
        + "    VariableTree\n"
        + "      ModifiersTree\n"
        + "      PrimitiveTypeTree\n"
        + "      IdentifierTree\n"
        + "    MethodTree\n"
        + "      ModifiersTree\n"
        + "      TypeParameters\n"
        + "      PrimitiveTypeTree\n"
        + "      IdentifierTree\n"
        + "      BlockTree\n"
        + "    MethodTree\n"
        + "      ModifiersTree\n"
        + "      TypeParameters\n"
        + "      PrimitiveTypeTree\n"
        + "      IdentifierTree\n"
        + "      BlockTree\n"
        + "    MethodTree\n"
        + "      ModifiersTree\n"
        + "      TypeParameters\n"
        + "      PrimitiveTypeTree\n"
        + "      IdentifierTree\n"
        + "      BlockTree\n"
        + "    ]\n"
        + "  ]\n");
  }
}
