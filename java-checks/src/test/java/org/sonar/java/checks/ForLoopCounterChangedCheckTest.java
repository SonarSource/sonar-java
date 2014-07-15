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
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class ForLoopCounterChangedCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ForLoopCounterChangedCheck.java"), new ForLoopCounterChangedCheck());
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(10).withMessage("Refactor the code in order to not assign to this loop counter from within the loop body.")
        .next().atLine(18)
        .next().atLine(19)
        .next().atLine(24)
        .next().atLine(27)
        .next().atLine(28)
        .next().atLine(30)
        .next().atLine(37)
        .next().atLine(38)
        .next().atLine(46)
        .next().atLine(47)
        .next().atLine(48)
        .next().atLine(49)
        .next().atLine(53)
        .next().atLine(62);
  }

}
