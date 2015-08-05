/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import java.io.File;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

public class SuppressWarningsCheckTest {

  @Test
  public void empty_list_of_warnings_then_any_suppressWarnings_is_an_issue() throws Exception {
    CheckMessagesVerifier.verify(getSourceFile("").getCheckMessages())
      .next().atLine(1).withMessage("Suppressing warnings is not allowed")
      .next().atLine(6)
      .next().atLine(10)
      .next().atLine(14)
      .next().atLine(18)
      .next().atLine(22)
      .noMore();
  }

  @Test
  public void list_of_warnings_with_syntax_error_then_any_suppressWarnings_is_an_issue() throws Exception {
    CheckMessagesVerifier.verify(getSourceFile("   ,   , ,,").getCheckMessages())
      .next().atLine(1).withMessage("Suppressing warnings is not allowed")
      .next().atLine(6)
      .next().atLine(10)
      .next().atLine(14)
      .next().atLine(18)
      .next().atLine(22)
      .noMore();
  }

  @Test
  public void only_one_warning_is_not_allowed() throws Exception {
    CheckMessagesVerifier.verify(getSourceFile("all").getCheckMessages())
      .next().atLine(1).withMessage("Suppressing the 'unused' warning is not allowed")
      .next().atLine(10).withMessage("Suppressing the 'unchecked, cast' warnings is not allowed")
      .next().atLine(22).withMessage("Suppressing the 'boxing' warning is not allowed")
      .noMore();
  }

  @Test
  public void warning_based_on_constants_are_ignored() throws Exception {
    CheckMessagesVerifier.verify(getSourceFile("boxing").getCheckMessages())
      .next().atLine(1).withMessage("Suppressing the 'unused' warning is not allowed")
      .next().atLine(6).withMessage("Suppressing the 'all' warning is not allowed")
      .next().atLine(10).withMessage("Suppressing the 'unchecked, cast' warnings is not allowed")
      .next().atLine(18).withMessage("Suppressing the 'all' warning is not allowed")
      .noMore();
  }

  @Test
  public void two_warnings_from_different_lines_are_not_allowed() throws Exception {
    CheckMessagesVerifier.verify(getSourceFile("unused, cast").getCheckMessages())
      .next().atLine(6).withMessage("Suppressing the 'all' warning is not allowed")
      .next().atLine(10).withMessage("Suppressing the 'unchecked' warning is not allowed")
      .next().atLine(18).withMessage("Suppressing the 'all' warning is not allowed")
      .next().atLine(22).withMessage("Suppressing the 'boxing' warning is not allowed")
      .noMore();
  }

  private SourceFile getSourceFile(String listOfWarnings) {
    SuppressWarningsCheck checker = new SuppressWarningsCheck();
    checker.warningsCommaSeparated = listOfWarnings;
    return JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SuppressWarningsCheck.java"), new VisitorsBridge(checker));
  }
}
