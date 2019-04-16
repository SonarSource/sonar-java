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
package org.sonar.java.checks.spring;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class SpringComponentWithNonAutowiredMembersCheckTest {

  private String basePath = "src/test/files/checks/spring/SpringComponentWithNonAutowiredMembersCheck/";
  private SpringComponentWithNonAutowiredMembersCheck check = new SpringComponentWithNonAutowiredMembersCheck();

  @Test
  public void default_annotations() {
    JavaCheckVerifier.verify(basePath + "DefaultAnnotations.java", check);
    JavaCheckVerifier.verifyNoIssueWithoutSemantic(basePath + "DefaultAnnotations.java", check);
  }

  @Test
  public void custom_annotations() {
    check.customInjectionAnnotations = "com.mycompany.myproject.MyController$MyInjectionAnnotation ,,";
    JavaCheckVerifier.verify(basePath + "CustomAnnotations.java", check);
  }

}
