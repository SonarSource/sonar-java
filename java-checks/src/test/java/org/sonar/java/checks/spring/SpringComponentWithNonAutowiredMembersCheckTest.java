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
package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class SpringComponentWithNonAutowiredMembersCheckTest {

  private String basePath = "checks/S3749_SpringComponentWithNonAutowiredMembersCheck/";

  @Test
  void default_annotations() {
    JavaFileScanner check = new SpringComponentWithNonAutowiredMembersCheck();
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(basePath + "S3749_DefaultAnnotations.java"))
      .withCheck(check)
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(basePath + "S3749_DefaultAnnotations.java"))
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void custom_annotations() {
    SpringComponentWithNonAutowiredMembersCheck check = new SpringComponentWithNonAutowiredMembersCheck();
    check.customInjectionAnnotations = "com.mycompany.myproject.MyController$MyInjectionAnnotation ,,";
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath(basePath + "S3749_CustomAnnotations.java"))
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

}
