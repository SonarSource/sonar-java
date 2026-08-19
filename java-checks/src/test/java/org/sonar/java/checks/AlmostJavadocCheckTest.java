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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class AlmostJavadocCheckTest {

  @Test
  void issue_message() {
    assertThat(AlmostJavadocCheck.MESSAGE)
      .isEqualTo("This comment contains Javadoc or HTML tags, but isn't started with a double asterisk (/**); is it meant to be Javadoc?");
  }


  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AlmostJavadocCheckSample.java"))
      .withCheck(new AlmostJavadocCheck())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AlmostJavadocCheckSample.java"))
      .withCheck(new AlmostJavadocCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void compact_source() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AlmostJavadocCheck_compactSource.java"))
      .withCheck(new AlmostJavadocCheck())
      .withJavaVersion(25)
      .verifyIssues();
  }
}
