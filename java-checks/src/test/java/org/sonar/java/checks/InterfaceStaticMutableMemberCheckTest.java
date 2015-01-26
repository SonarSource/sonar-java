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

public class InterfaceStaticMutableMemberCheckTest {

  @Test
  public void test() {
    InterfaceStaticMutableMemberCheck check = new InterfaceStaticMutableMemberCheck();
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/InterfaceStaticMutableMemberCheck.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(6).withMessage("Move \"MY_ARRAY\" to a class and lower its visibility")
      .next().atLine(7).withMessage("Move \"MY_COLLECTION\" to a class and lower its visibility")
      .next().atLine(8).withMessage("Move \"MY_2ND_COLLECTION\" to a class and lower its visibility")
      .next().atLine(9).withMessage("Move \"MY_LIST\" to a class and lower its visibility")
      .next().atLine(10).withMessage("Move \"MY_2ND_LIST\" to a class and lower its visibility")
      .next().atLine(11).withMessage("Move \"MY_DATE\" to a class and lower its visibility")
      .noMore();
  }

}
