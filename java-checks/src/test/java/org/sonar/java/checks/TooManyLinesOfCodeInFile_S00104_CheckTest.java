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

import static org.fest.assertions.Assertions.assertThat;

public class TooManyLinesOfCodeInFile_S00104_CheckTest {

  @Test
  public void testDefault() {
    assertThat(new TooManyLinesOfCodeInFile_S00104_Check().maximum).isEqualTo(1000);
  }

  @Test
  public void test() {
    TooManyLinesOfCodeInFile_S00104_Check check = new TooManyLinesOfCodeInFile_S00104_Check();
    check.maximum = 1;
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/TooManyLinesOfCode.java", "This file has 11 lines, which is greater than 1 authorized. Split it into smaller files.", check);
  }

  @Test
  public void test2() {
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/TooManyLinesOfCode.java", new TooManyLinesOfCodeInFile_S00104_Check());
  }

}
