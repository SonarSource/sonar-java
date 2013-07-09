/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.squid.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.JavaAstScanner;
import org.sonar.squid.api.SourceFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class ClassVariableVisibilityCheckTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    assertThat(new ClassVariableVisibilityCheck().authorizedVisibility).isEqualTo("private");
  }

  @Test
  public void detected_with_private() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ClassVariableVisibilityCheck.java"), new ClassVariableVisibilityCheck());
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(3).withMessage("Make this class member private visible.")
        .next().atLine(4)
        .next().atLine(5)
        .next().atLine(7)
        .next().atLine(8);
  }

  @Test
  public void detected_with_package() {
    ClassVariableVisibilityCheck check = new ClassVariableVisibilityCheck();
    check.authorizedVisibility = "package";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ClassVariableVisibilityCheck.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(4).withMessage("Make this class member package visible.")
        .next().atLine(5)
        .next().atLine(7)
        .next().atLine(8);
  }

  @Test
  public void detected_with_protected() {
    ClassVariableVisibilityCheck check = new ClassVariableVisibilityCheck();
    check.authorizedVisibility = "protected";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ClassVariableVisibilityCheck.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(5).withMessage("Make this class member protected visible.")
        .next().atLine(7)
        .next().atLine(8);
  }

  @Test
  public void should_fail_with_bad_authorized_visibility() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Unexpected authorized visibility 'public', expected one of: 'private', 'package' or 'protected'.");

    ClassVariableVisibilityCheck check = new ClassVariableVisibilityCheck();
    check.authorizedVisibility = "public";
    check.init();
  }

}
