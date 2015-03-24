/*
 * SonarQube Java
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class PreparedStatementAndResultSetCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/PreparedStatementAndResultSetCheck.java"),
      new VisitorsBridge(new PreparedStatementAndResultSetCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(12).withMessage("PreparedStatement indices start at 1.")
      .next().atLine(13).withMessage("This \"PreparedStatement\" only has 2 parameters.")
      .next().atLine(18).withMessage("ResultSet indices start at 1.")
      .next().atLine(19).withMessage("ResultSet indices start at 1.")
      .next().atLine(26).withMessage("PreparedStatement indices start at 1.")
      .next().atLine(27).withMessage("This \"PreparedStatement\" has no parameters.")
      .next().atLine(33).withMessage("PreparedStatement indices start at 1.")
      .next().atLine(38).withMessage("PreparedStatement indices start at 1.")
      .next().atLine(43).withMessage("PreparedStatement indices start at 1.")
      .next().atLine(50).withMessage("PreparedStatement indices start at 1.")
      .next().atLine(54).withMessage("PreparedStatement indices start at 1.")
      .noMore();
  }

}
