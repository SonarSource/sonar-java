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

import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;

public class FinalizeFieldsSetCheckTest {

  private final FinalizeFieldsSetCheck check = new FinalizeFieldsSetCheck();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FinalizeFieldsSetCheck.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(8).withMessage("Remove this nullification of \"myString\".")
      .next().atLine(9).withMessage("Remove this nullification of \"myInteger\".")
      .next().atLine(10).withMessage("Remove this nullification of \"myObject\".")
      .next().atLine(18)
      .next().atLine(19)
      .next().atLine(20)
      .next().atLine(32)
      .next().atLine(42)
      .next().atLine(43)
      .next().atLine(46).withMessage("Remove this nullification of \"myArrayOfStrings\".")
      .next().atLine(51)
      .next().atLine(52)
      .next().atLine(53)
      .next().atLine(54)
      .next().atLine(67)
      .noMore();
  }

}
