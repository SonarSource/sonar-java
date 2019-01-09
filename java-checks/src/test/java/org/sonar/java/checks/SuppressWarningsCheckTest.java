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

public class SuppressWarningsCheckTest {

  @Test
  public void empty_list_of_warnings_then_any_suppressWarnings_is_an_issue() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/SuppressWarningsCheck/test1.java", getCheck(""));
  }

  @Test
  public void list_of_warnings_with_syntax_error_then_any_suppressWarnings_is_an_issue() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/SuppressWarningsCheck/test1.java", getCheck("   ,   , ,,"));
  }

  @Test
  public void only_one_warning_is_not_allowed() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/SuppressWarningsCheck/only_one_warning_is_not_allowed.java", getCheck("all"));
  }

  @Test
  public void warning_based_on_constants_are_ignored() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/SuppressWarningsCheck/warning_based_on_constants_are_ignored.java", getCheck("boxing"));
  }

  @Test
  public void two_warnings_from_different_lines_are_not_allowed() throws Exception {
    JavaCheckVerifier.verify("src/test/files/checks/SuppressWarningsCheck/two_warnings_from_different_lines_are_not_allowed.java", getCheck("unused, cast"));
  }

  private static SuppressWarningsCheck getCheck(String parameter) {
    SuppressWarningsCheck check = new SuppressWarningsCheck();
    check.warningsCommaSeparated = parameter;
    return check;
  }
}
