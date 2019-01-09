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

public class DisallowedMethodCheckTest {

  @Test
  public void detected() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    disallowedMethodCheck.setMethodName("foo");
    disallowedMethodCheck.setArgumentTypes("int, long, java.lang.String[]");
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedMethodCheck/detected.java", disallowedMethodCheck);
  }

  @Test
  public void all_overloads() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    disallowedMethodCheck.setMethodName("foo");
    disallowedMethodCheck.setAllOverloads(true);
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedMethodCheck/detected.java", disallowedMethodCheck);
  }

  @Test
  public void empty_parameters() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    disallowedMethodCheck.setMethodName("bar");
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedMethodCheck/empty_parameters.java", disallowedMethodCheck);
  }

  @Test
  public void empty_type_definition() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setMethodName("bar");
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedMethodCheck/empty_type_definition.java", disallowedMethodCheck);
  }

  @Test
  public void empty_method_name() {
    DisallowedMethodCheck disallowedMethodCheck = new DisallowedMethodCheck();
    disallowedMethodCheck.setClassName("A");
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/DisallowedMethodCheck/empty_method_name.java", disallowedMethodCheck);
  }
}
