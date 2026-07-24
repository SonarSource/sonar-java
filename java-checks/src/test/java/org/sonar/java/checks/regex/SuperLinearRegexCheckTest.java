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
package org.sonar.java.checks.regex;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class SuperLinearRegexCheckTest {

  @Test
  void test_reproducer_unset() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/SuperLinearRegexCheckReproducer.java"))
      .withCheck(new SuperLinearRegexCheck())
      .verifyNoIssues();
  }

  @Test
  void test_reproducer_9() {
    CheckVerifier.newVerifier()
      .withJavaVersion(9)
      .onFile(mainCodeSourcesPath("checks/regex/SuperLinearRegexCheckReproducer.java"))
      .withCheck(new SuperLinearRegexCheck())
      .verifyNoIssues();
  }

  @Test
  void test_reproducer_8() {
    CheckVerifier.newVerifier()
      .withJavaVersion(8)
      .onFile(mainCodeSourcesPath("checks/regex/SuperLinearRegexCheckReproducer.java"))
      .withCheck(new SuperLinearRegexCheck())
      .verifyNoIssues();
  }

  @Test
  void test_java_version_unset() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/regex/SuperLinearRegexCheckSample.java"))
      .withCheck(new SuperLinearRegexCheck())
      .verifyIssues();
  }

  @Test
  void test_java_version_9() {
    CheckVerifier.newVerifier()
      .withJavaVersion(9)
      .onFile(mainCodeSourcesPath("checks/regex/SuperLinearRegexCheckSample.java"))
      .withCheck(new SuperLinearRegexCheck())
      .verifyIssues();
  }

  @Test
  void test_java_version_8() {
    CheckVerifier.newVerifier()
      .withJavaVersion(8)
      .onFile(mainCodeSourcesPath("checks/regex/SuperLinearRegexCheckJava8.java"))
      .withCheck(new SuperLinearRegexCheck())
      .verifyIssues();
  }

}
