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

public class ArchitectureCheckTest {

  @Test
  public void test() {
    ArchitectureCheck check = new ArchitectureCheck();
    check.fromClasses = "**.targets.**";
    check.toClasses = "java.**.Pattern,java.io.File";
    JavaCheckVerifier.verify("src/test/files/checks/Architecture.java", check);
  }

  @Test
  public void test_self_reference() {
    ArchitectureCheck check = new ArchitectureCheck();
    check.fromClasses = "**.targets.**";
    check.toClasses = "**.targets.**";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/ArchitectureSelf.java", check);
  }

  @Test
  public void testOk() {
    ArchitectureCheck check = new ArchitectureCheck();
    check.fromClasses = "com.**";
    check.toClasses = "java.**.Pattern";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/ArchitectureOk.java", check);
  }

  @Test
  public void testSkipFolder() {
    ArchitectureCheck check = new ArchitectureCheck();
    check.fromClasses = "org.*.util.**";
    check.toClasses = "java.lang.String";
    JavaCheckVerifier.verify("src/test/files/checks/ArchitectureSkipSingleFolder.java", check);
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/ArchitectureSkipSingleFolderOK.java", check);
  }
}
