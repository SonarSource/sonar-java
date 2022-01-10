/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class FileHeaderCheckTest {

  @Test
  void test() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class1.java"))
      .withCheck(check)
      .verifyNoIssues();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 20\\d\\d";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class1.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyNoIssues();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyNoIssues();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\n// foo";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyNoIssues();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r// foo";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyNoIssues();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\r// foo";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo\n\n\n\n\n\n\n\n\n\ngfoo";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class2.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "/*foo http://www.example.org*/";
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Class3.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void regex() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "^// copyright \\d\\d\\d";
    check.isRegularExpression = true;
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Regex1.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");
    // Check that the regular expression is compiled once
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Regex1.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\n// mycompany";
    check.isRegularExpression = true;

    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Regex2.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\r?\\n// mycompany";
    check.isRegularExpression = true;
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Regex3.java"))
      .withCheck(check)
      .verifyNoIssues();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright \\d{4}\\n// mycompany";
    check.isRegularExpression = true;
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/FileHeaderCheck/Regex4.java"))
      .withCheck(check)
      .verifyIssueOnFile("Add or update the header of this file.");
  }

  @Test
  void should_fail_with_bad_regular_expression() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "**";
    check.isRegularExpression = true;
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);

    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> check.scanFile(context));
    assertThat(e.getMessage()).isEqualTo("[FileHeaderCheck] Unable to compile the regular expression: **");
  }

}
