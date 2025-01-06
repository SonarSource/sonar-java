/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.security;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class VerifiedServerHostnamesCheckTest {

  public static final String TEST_FOLDER = "checks/security/VerifiedServerHostnamesCheck/";

  @Test
  void hostname_verifier() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FOLDER + "HostnameVerifier.java"))
      .withCheck(new VerifiedServerHostnamesCheck())
      .verifyIssues();
  }

  @Test
  void java_mail_session() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FOLDER + "JavaMailSession.java"))
      .withCheck(new VerifiedServerHostnamesCheck())
      .verifyIssues();
  }

  @Test
  void apache_common_email() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(TEST_FOLDER + "ApacheCommonEmail.java"))
      .withCheck(new VerifiedServerHostnamesCheck())
      .verifyIssues();
  }

}
