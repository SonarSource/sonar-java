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
    verify("Junit3");
  }

  @Test
  public void junit4() {
    verify("Junit4");
  }

  @Test
  public void assertJ() {
    verify("AssertJ");
  }

  @Test
  public void hamcrest() {
    verify("Hamcrest");
  }

  @Test
  public void spring() {
    verify("Spring");
  }

  @Test
  public void easyMock() {
    verify("EasyMock");
  }

  @Test
  public void truth() {
    verify("Truth");
  }

  @Test
  public void restAssured() {
    verify("RestAssured");
  }

  @Test
  public void mockito() {
    verify("Mockito");
  }

  @Test
  public void jMock() {
    verify("JMock");
  }

  private void verify(String framework) {
    JavaCheckVerifier.verify("src/test/files/checks/AssertionsInTestsCheck/" + framework + ".java", check);
  }
}
