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

public class PrintfCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/PrintfCheck.java"), new VisitorsBridge(new PrintfCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(9).withMessage("An 'int' is expected rather than a String.")
        .next().atLine(10).withMessage("Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here")
        .next().atLine(11).withMessage("X is not a supported time conversion character")
        .next().atLine(12).withMessage("2nd argument is not used.")
        .next().atLine(13).withMessage("3rd argument is not used.")
        .next().atLine(14).withMessage("Not enough arguments.")
        .next().atLine(15).withMessage("%n should be used in place of \\n to produce the platform-specific line separator.")
        .next().atLine(16).withMessage("The argument index '<' refers to the previous format specifier but there isn't one.")
        .next().atLine(17).withMessage("Directly inject the boolean value.")
        .next().atLine(24).withMessage("An 'int' is expected rather than a String.")
        .next().atLine(25).withMessage("An 'int' is expected rather than a String.")
        .next().atLine(26).withMessage("An 'int' is expected rather than a String.")
        .next().atLine(27).withMessage("An 'int' is expected rather than a String.")
        .next().atLine(28).withMessage("An 'int' is expected rather than a String.")
        .next().atLine(34).withMessage("4th argument is not used.")
    ;
  }
}
