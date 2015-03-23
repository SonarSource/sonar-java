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

public class RedundantTypeCastCheckTest {
  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/RedundantTypeCastCheck.java"), new VisitorsBridge(new RedundantTypeCastCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(9).withMessage("Remove this unnecessary cast to \"List\".")
        .next().atLine(10).withMessage("Remove this unnecessary cast to \"List\".")
        .next().atLine(11).withMessage("Remove this unnecessary cast to \"List\".")
        .next().atLine(13).withMessage("Remove this unnecessary cast to \"String\".")
        .next().atLine(14).withMessage("Remove this unnecessary cast to \"A\".")
        .next().atLine(15).withMessage("Remove this unnecessary cast to \"A[][]\".")
        .next().atLine(24).withMessage("Remove this unnecessary cast to \"int\".")
        .next().atLine(38).withMessage("Remove this unnecessary cast to \"Object\".")
        .next().atLine(44).withMessage("Remove this unnecessary cast to \"T[]\".")
        .next().atLine(47).withMessage("Remove this unnecessary cast to \"String[]\".")
    ;
  }
}
