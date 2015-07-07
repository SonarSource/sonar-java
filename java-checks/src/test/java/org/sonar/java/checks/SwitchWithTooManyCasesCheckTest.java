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

import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;

public class SwitchWithTooManyCasesCheckTest {


  @Test
  public void defaultValue() {
    SwitchWithTooManyCasesCheck check = new SwitchWithTooManyCasesCheck();
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SwitchWithTooManyCasesCheck.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(3).withMessage("Reduce the number of switch cases from 35 to at most 30.")
        .noMore();
  }

  @Test
  public void test() {
    SwitchWithTooManyCasesCheck check = new SwitchWithTooManyCasesCheck();
    check.maximumCases = 5;
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SwitchWithTooManyCasesCheck.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(3).withMessage("Reduce the number of switch cases from 35 to at most 5.")
        .next().atLine(44).withMessage("Reduce the number of switch cases from 6 to at most 5.")
        .noMore();
  }



}
