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

class InsecureCreateTempFileCheckTest {

  @Test
  void no_version() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/InsecureCreateTempFileCheck_no_version.java"))
      .withCheck(new InsecureCreateTempFileCheck())
      .verifyIssues();
  }

  @Test
  void java_7() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/InsecureCreateTempFileCheckSample.java"))
      .withCheck(new InsecureCreateTempFileCheck())
      .withJavaVersion(7)
      .verifyIssues();
  }

}
