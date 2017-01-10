/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

public class AssertionsInTestsCheckTest {

  private AssertionsInTestsCheck check = new AssertionsInTestsCheck();

  @Test
  public void junit3() {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheckJunit3.java", check);
  }

  @Test
  public void junit4() {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheckJunit4.java", check);
  }

  @Test
  public void assertJ() {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheckAssertJ.java", check);
  }

  @Test
  public void hamcrest() {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheckHamcrest.java", check);
  }

  @Test
  public void spring() {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheckSpring.java", check);
  }

  @Test
  public void easyMock() {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheckEasyMock.java", check);
  }

  @Test
  public void truth() {
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/AssertionsInTestsCheckTruth.java", check);
  }

}
