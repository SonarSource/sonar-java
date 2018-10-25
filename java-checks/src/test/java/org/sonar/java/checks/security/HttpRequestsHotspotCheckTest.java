/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.java.model.JavaVersionImpl;

public class HttpRequestsHotspotCheckTest {

  @Test
  public void test() {
    int javaVersion = JavaVersionImpl.fromString(System.getProperty("java.specification.version")).asInt();
    if (javaVersion >= 11) {
      JavaCheckVerifier.verify("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck_java11.java", new HttpRequestsHotspotCheck());
    } else if (javaVersion == 10) {
      JavaCheckVerifier.verify("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck_java10.java", new HttpRequestsHotspotCheck());
    } else if (javaVersion == 9) {
      JavaCheckVerifier.verify("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck_java9.java", new HttpRequestsHotspotCheck());
    }

    JavaCheckVerifier.verify("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck.java", new HttpRequestsHotspotCheck());
  }

  @Test
  public void noSemantic() {
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck.java", new HttpRequestsHotspotCheck());
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck_java9.java", new HttpRequestsHotspotCheck());
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck_java10.java", new HttpRequestsHotspotCheck());
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/security/HttpRequestsHotspotCheck/HttpRequestsHotspotCheck_java11.java", new HttpRequestsHotspotCheck());
  }

}
