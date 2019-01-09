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
import org.sonar.java.model.JavaVersionImpl;

public class RunFinalizersCheckTest {

  @Test
  public void test() {
    int javaVersion = JavaVersionImpl.fromString(System.getProperty("java.specification.version")).asInt();
    if (javaVersion < 11) {
      JavaCheckVerifier.verify("src/test/files/checks/RunFinalizersCheck.java", new RunFinalizersCheck());
      JavaCheckVerifier.verify("src/test/files/checks/RunFinalizersCheck.java", new RunFinalizersCheck(), javaVersion);
      JavaCheckVerifier.verifyNoIssue("src/test/files/checks/RunFinalizersCheck_no_issue.java", new RunFinalizersCheck(), 11);
    } else {
      // No issue raised starting JDK 11 as the related APIs were removed from JDK and cannot be resolved
      JavaCheckVerifier.verifyNoIssue("src/test/files/checks/RunFinalizersCheck_no_issue.java", new RunFinalizersCheck());
      JavaCheckVerifier.verifyNoIssue("src/test/files/checks/RunFinalizersCheck_no_issue.java", new RunFinalizersCheck(), javaVersion);
      JavaCheckVerifier.verifyNoIssue("src/test/files/checks/RunFinalizersCheck_no_issue.java", new RunFinalizersCheck(), 10);
    }
  }

  @Test
  public void noSemantic() {
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/RunFinalizersCheck.java", new RunFinalizersCheck());
  }

}
