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

public class DisallowedConstructorCheckTest {

  @Test
  public void detected() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    disallowedConstructorCheck.setClassName("A");
    disallowedConstructorCheck.setArgumentTypes("int, long, java.lang.String[]");
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedConstructorCheck/detected.java", disallowedConstructorCheck);
  }

  @Test
  public void all_overloads() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    disallowedConstructorCheck.setClassName("A");
    disallowedConstructorCheck.setAllOverloads(true);
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedConstructorCheck/detected_all_overload.java", disallowedConstructorCheck);
  }

  @Test
  public void empty_parameters() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    disallowedConstructorCheck.setClassName("A");
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedConstructorCheck/empty_parameters.java", disallowedConstructorCheck);
  }

  @Test
  public void empty_type_definition() {
    DisallowedConstructorCheck disallowedConstructorCheck = new DisallowedConstructorCheck();
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/DisallowedConstructorCheck/empty_type_definition.java", disallowedConstructorCheck);
  }

}
