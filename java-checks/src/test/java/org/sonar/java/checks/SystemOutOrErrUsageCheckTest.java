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

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class SystemOutOrErrUsageCheckTest {
  @Test
  void test_sout() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/SystemOutOrErrUsageCheckSample.java"))
      .withCheck(new SystemOutOrErrUsageCheck())
      .verifyIssues();
  }

  @Test
  void test_sout_compact_source_file() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/SystemOutOrErrUsageCheckCompactOnlyMainSample.java"))
      .withCheck(new SystemOutOrErrUsageCheck())
      .verifyNoIssues();
  }

  @Test
  void test_io() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/IoPrintlnUsageCheckSample.java"))
      .withCheck(new SystemOutOrErrUsageCheck())
      .verifyIssues();
  }

  @Test
  void test_io_compact_source_file() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/IoPrintlnUsageCheckCompactSample.java"))
      .withCheck(new SystemOutOrErrUsageCheck())
      .verifyNoIssues();
  }

  @Test
  void test_compact_source_file_with_regular_class() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/SystemOutOrErrUsageCheckCompactWithClassSample.java"))
      .withCheck(new SystemOutOrErrUsageCheck())
      .verifyNoIssues();
  }
}
