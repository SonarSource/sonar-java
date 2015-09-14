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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class BadLocalVariableName_S00117_CheckTest {

  private final BadLocalVariableName_S00117_Check check = new BadLocalVariableName_S00117_Check();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/BadLocalVariableName.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(3).withMessage("Rename this local variable name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.")
      .next().atLine(5)
      .next().atLine(9)
      .next().atLine(15)
      .next().atLine(16)
      .next().atLine(22)
      .next().atLine(28)
      .noMore();
  }

  @Test
  public void test2() {
    check.format = "^[a-zA-Z0-9_]*$";
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/BadLocalVariableName.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();
  }

}
