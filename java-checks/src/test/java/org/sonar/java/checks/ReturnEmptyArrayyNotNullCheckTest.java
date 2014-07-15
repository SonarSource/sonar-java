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

public class ReturnEmptyArrayyNotNullCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ReturnEmptyArrayyNotNullCheck.java"), new ReturnEmptyArrayyNotNullCheck());
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(16).withMessage("Return an empty array instead of null.")
        .next().atLine(25)
        .next().atLine(35)
        .next().atLine(40)
        .next().atLine(44)
        .next().atLine(48)
        .next().atLine(53).withMessage("Return an empty collection instead of null.")
        .next().atLine(57).withMessage("Return an empty array instead of null.")
        .next().atLine(61)
        .next().atLine(72)
        .next().atLine(80)
        .next().atLine(88)
    ;
  }

}
