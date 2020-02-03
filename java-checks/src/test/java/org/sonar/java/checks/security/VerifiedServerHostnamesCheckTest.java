/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class VerifiedServerHostnamesCheckTest {

  public static final String TEST_FOLDER = "src/test/files/checks/security/VerifiedServerHostnamesCheck/";

  @Test
  public void hostname_verifier() {
    JavaCheckVerifier.verify(TEST_FOLDER + "HostnameVerifier.java", new VerifiedServerHostnamesCheck());
  }

  @Test
  public void java_mail_session() {
    JavaCheckVerifier.verify(TEST_FOLDER + "JavaMailSession.java", new VerifiedServerHostnamesCheck());
  }

  @Test
  public void apache_common_email() {
    JavaCheckVerifier.verify(TEST_FOLDER + "ApacheCommonEmail.java", new VerifiedServerHostnamesCheck());
  }

}
