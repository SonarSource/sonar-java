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

public class CustomSerializationMethodCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/CustomSerializationMethodCheck.java"),
      new VisitorsBridge(new CustomSerializationMethodCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(17).withMessage("Make \"writeObject\" \"private\".")
      .next().atLine(18)
      .next().atLine(19)
      .next().atLine(23).withMessage("The \"static\" modifier should not be applied to \"writeObject\".")
      .next().atLine(24)
      .next().atLine(25)
      .next().atLine(26)
      .next().atLine(27)
      .next().atLine(39).withMessage("\"writeReplace\" should return \"java.lang.Object\".")
      .next().atLine(40);
  }

}
