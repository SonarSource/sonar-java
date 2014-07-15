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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class TooManyStatementsPerLine_S00122_CheckTest {

  private TooManyStatementsPerLine_S00122_Check check = new TooManyStatementsPerLine_S00122_Check();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/TooManyStatementsPerLine.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(5).withMessage("At most one statement is allowed per line, but 2 statements were found on this line.")
        .next().atLine(9)
        .next().atLine(17)
        .next().atLine(19).withMessage("At most one statement is allowed per line, but 2 statements were found on this line.")
        .next().atLine(37).withMessage("At most one statement is allowed per line, but 2 statements were found on this line.")
        .next().atLine(38).withMessage("At most one statement is allowed per line, but 2 statements were found on this line.")
        .next().atLine(42).withMessage("At most one statement is allowed per line, but 2 statements were found on this line.")
        .noMore();
  }

}
