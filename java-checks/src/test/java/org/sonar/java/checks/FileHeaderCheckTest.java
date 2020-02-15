/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.sonar.java.CheckTestUtils.testSourcesPath;

public class FileHeaderCheckTest {

  @Test
  public void test() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Class1.java"), check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 20\\d\\d";
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Class1.java"), "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"), "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012";
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Class2.java"), check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo";
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Class2.java"), check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\n// foo";
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Class2.java"), check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r// foo";
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Class2.java"), check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\r// foo";
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"), "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo\n\n\n\n\n\n\n\n\n\ngfoo";
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"), "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "/*foo http://www.example.org*/";
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Class3.java"), check);
  }

  @Test
  public void regex() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "^// copyright \\d\\d\\d";
    check.isRegularExpression = true;
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Regex1.java"), "Add or update the header of this file.", check);
    // Check that the regular expression is compiled once
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Regex1.java"), "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\n// mycompany";
    check.isRegularExpression = true;

    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Regex2.java"), "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\r?\\n// mycompany";
    check.isRegularExpression = true;
    JavaCheckVerifier.verifyNoIssue(testSourcesPath("checks/FileHeaderCheck/Regex3.java"), check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\n// mycompany";
    check.isRegularExpression = true;
    JavaCheckVerifier.verifyIssueOnFile(testSourcesPath("checks/FileHeaderCheck/Regex4.java"), "Add or update the header of this file.", check);
  }

  @Test
  public void should_fail_with_bad_regular_expression() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "**";
    check.isRegularExpression = true;

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> check.scanFile(mock(JavaFileScannerContext.class)));
    assertThat(e.getMessage()).isEqualTo("[FileHeaderCheck] Unable to compile the regular expression: **");
  }

}
