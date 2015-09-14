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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class ImmediateReverseBoxingCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ImmediateReverseBoxingCheck.java"),
      new VisitorsBridge(new ImmediateReverseBoxingCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(7).withMessage("Remove the boxing of \"int1\".")
      .next().atLine(8).withMessage("Remove the boxing to \"Integer\".")
      .next().atLine(9).withMessage("Remove the boxing of \"int1\".")
      .next().atLine(10).withMessage("Remove the boxing to \"Integer\".")
      .next().atLine(11).withMessage("Remove the boxing of \"int1\".")
      .next().atLine(14)
      .next().atLine(15)
      .next().atLine(24).withMessage("Remove the unboxing of \"integer1\".")
      .next().atLine(27).withMessage("Remove the unboxing from \"Integer\".")
      .next().atLine(28)
      .next().atLine(31)
      .next().atLine(34)
      .next().atLine(36)
      .next().atLine(44).withMessage("Remove the boxing of \"b\".")
      .next().atLine(45)
      .next().atLine(46).withMessage("Remove the boxing to \"Double\".")
      .next().atLine(47)
      .next().atLine(48)
      .next().atLine(49)
      .next().atLine(50)
      .next().atLine(51)
      .next().atLine(52)
      .next().atLine(53)
      .next().atLine(54)
      .next().atLine(55);
  }

}
