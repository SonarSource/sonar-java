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

class DateAndTimesCheckTest {

  @Test
  void test_multiple_imports() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DateAndTimesCheck/MultipleImports.java")
      .withCheck(new DateAndTimesCheck())
      .withJavaVersion(8)
      .verifyIssueOnFile("Use the \"java.time\" API for date and time.");
  }

  @Test
  void test_joda_time() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DateAndTimesCheck/JodaTime.java")
      .withCheck(new DateAndTimesCheck())
      .withJavaVersion(8)
      .verifyIssueOnFile("Use the \"java.time\" API for date and time.");
  }

  @Test
  void test_wildcard_import() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DateAndTimesCheck/WildcardImport.java")
      .withCheck(new DateAndTimesCheck())
      .withJavaVersion(8)
      .verifyIssueOnFile("Use the \"java.time\" API for date and time.");
  }

  @Test
  void test_static_import() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DateAndTimesCheck/StaticImport.java")
      .withCheck(new DateAndTimesCheck())
      .withJavaVersion(8)
      .verifyIssueOnFile("Use the \"java.time\" API for date and time.");
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DateAndTimesCheck/MultipleImports.java")
      .withCheck(new DateAndTimesCheck())
      .withJavaVersion(8)
      .withoutSemantic()
      .verifyIssueOnFile("Use the \"java.time\" API for date and time.");
  }

}
