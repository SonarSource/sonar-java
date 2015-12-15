/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

public class TooLongLine_S00103_CheckTest {

  TooLongLine_S00103_Check check = new TooLongLine_S00103_Check();

  @Test
  public void test() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verify("src/test/files/checks/TooLongLine_S00103_Check/LineLength.java", check);
  }

  @Test
  public void test_with_empty_import_on_first_line() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verify("src/test/files/checks/TooLongLine_S00103_Check/LineLengthEmptyStatementInImport.java", check);
  }

  @Test
  public void test_with_no_import() {
    check.maximumLineLength = 20;
    JavaCheckVerifier.verify("src/test/files/checks/TooLongLine_S00103_Check/LineLengthNoImport.java", check);
  }
}
