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
package org.sonar.java.checks.naming;

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class BadMethodNameCheckTest {

  @Test
  public void test() {
    JavaCheckVerifier.verify("src/test/files/checks/naming/BadMethodName.java", new BadMethodNameCheck());
  }

  @Test
  public void test2() {
    BadMethodNameCheck check = new BadMethodNameCheck();
    check.format = "^[a-zA-Z0-9]*$";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/naming/BadMethodNameCustom.java", check);
  }

  @Test
  public void testOverrideWithoutAnnotation() throws Exception {
    BadMethodNameCheck check = new BadMethodNameCheck();
    check.format = "^[A-Z0-9]*$";
    JavaCheckVerifier.verify("src/test/files/checks/naming/BadMethodNameCustomNoncompliant.java", check);
  }
}
