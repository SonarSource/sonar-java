/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
