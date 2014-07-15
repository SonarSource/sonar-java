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

import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class LoggersDeclarationCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/LoggersDeclarationCheck.java"), new VisitorsBridge(new LoggersDeclarationCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(5)
      .next().atLine(6)
      .next().atLine(7)
      .next().atLine(9)
      .next().atLine(14).withMessage("Rename the \"foo\" logger to comply with the format \"LOG(?:GER)?\".")
      .next().atLine(20).withMessage("Make the \"foo\" logger private static final and rename it to comply with the format \"LOG(?:GER)?\".")
      .next().atLine(23).withMessage("Make the \"LOG\" logger private static final.");
  }

  @Test
  public void custom() {
    LoggersDeclarationCheck check = new LoggersDeclarationCheck();
    check.format = ".*";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/LoggersDeclarationCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(5)
      .next().atLine(6)
      .next().atLine(7)
      .next().atLine(20)
      .next().atLine(23);
  }

}
