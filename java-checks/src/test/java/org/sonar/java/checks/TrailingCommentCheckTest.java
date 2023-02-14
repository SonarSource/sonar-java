/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class TrailingCommentCheckTest {

  @Test
  void detected() {
    TrailingCommentCheck check = new TrailingCommentCheck();
    assertThat(check.legalCommentPattern).isEqualTo("^\\s*+[^\\s]++$");
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/TrailingCommentCheck.java"))
      .withCheck(check)
      .verifyIssues();
    check.legalCommentPattern = "";
    // parameter has changed but regexp is not recompiled, so we find the same issues.
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/TrailingCommentCheck.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void custom() {
    TrailingCommentCheck check = new TrailingCommentCheck();
    check.legalCommentPattern = "";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/TrailingCommentCheckCustom.java"))
      .withCheck(check)
      .verifyIssues();
  }

}
