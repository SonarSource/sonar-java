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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.mockito.Mockito.mock;

public class FileHeaderCheckTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Class1.java", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 20\\d\\d";
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Class1.java", "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Class2.java", "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Class2.java", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Class2.java", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\n// foo";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Class2.java", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r// foo";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Class2.java", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\r// foo";
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Class2.java", "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo\n\n\n\n\n\n\n\n\n\ngfoo";
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Class2.java", "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "/*foo http://www.example.org*/";
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Class3.java", check);
  }

  @Test
  public void regex() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "^// copyright \\d\\d\\d";
    check.isRegularExpression = true;
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Regex1.java", "Add or update the header of this file.", check);
    // Check that the regular expression is compiled once
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Regex1.java", "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\n// mycompany";
    check.isRegularExpression = true;

    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Regex2.java", "Add or update the header of this file.", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\r?\\n// mycompany";
    check.isRegularExpression = true;
    JavaCheckVerifier.verifyNoIssue("src/test/files/checks/FileHeaderCheck/Regex3.java", check);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\n// mycompany";
    check.isRegularExpression = true;
    JavaCheckVerifier.verifyIssueOnFile("src/test/files/checks/FileHeaderCheck/Regex4.java", "Add or update the header of this file.", check);
  }

  @Test
  public void should_fail_with_bad_regular_expression() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("[" + FileHeaderCheck.class.getSimpleName() + "] Unable to compile the regular expression: *");

    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "**";
    check.isRegularExpression = true;
    check.scanFile(mock(JavaFileScannerContext.class));
  }

}
