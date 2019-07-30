/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class UndocumentedApiCheckTest {

  @Test
  public void test() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    assertThat(check.forClasses).isEqualTo("**.api.**");
    assertThat(check.exclusion).isEqualTo("**.internal.**");
    JavaCheckVerifier.verify("src/test/files/checks/UndocumentedApiCheck/UndocumentedApi.java", check);
  }

  @Test
  public void testMissingConfiguration() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = null;
    check.exclusion = null;
    JavaCheckVerifier.verify("src/test/files/checks/UndocumentedApiCheck/UndocumentedApi.java", check);
  }

  @Test
  public void no_issue_without_Semantic() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/UndocumentedApiCheck/UndocumentedApi.java", check);
  }

  @Test
  public void custom() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "**.open.**";
    check.exclusion = "";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiCustom.java", check);
  }

  @Test
  public void testExclusion() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";
    check.exclusion = "**.internal.**";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiExclusion.java", check);
  }

  @Test
  public void testIncompleteJavadoc() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";
    JavaCheckVerifier.verify("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiIncomplete.java", check);
  }

  @Test
  public void testInvalidDeclaredException() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/UndocumentedApiCheck/UndocumentedApiInvalidException.java", check);
  }
}

