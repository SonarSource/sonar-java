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

public class DefaultPackageCheckTest {

  @Test
  public void without_package() {
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/EmptyFile.java", "Move this file to a named package.", new DefaultPackageCheck());
  }

  @Test
  public void with_package() {
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/WithPackage.java", new DefaultPackageCheck());
  }

  @Test
  public void with_module() {
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/WithModule.java", new DefaultPackageCheck());
  }

  @Test
  public void test_parsing_error_file() {
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/ParsingError.java", new DefaultPackageCheck());
  }

}
