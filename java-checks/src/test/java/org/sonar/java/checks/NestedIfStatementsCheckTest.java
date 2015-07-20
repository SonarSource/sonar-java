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

import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class NestedIfStatementsCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    NestedIfStatementsCheck check = new NestedIfStatementsCheck();
    assertThat(check.max).isEqualTo(3);

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/NestedIfStatementsCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(19).withMessage("Refactor this code to not nest more than 3 if/for/while/switch/try statements.")
      .next().atLine(41)
      .next().atLine(47)
      .next().atLine(57)
      .next().atLine(60)
      .next().atLine(63)
      .next().atLine(66)
      .next().atLine(69)
      .next().atLine(72)
      .next().atLine(75);
  }

  @Test
  public void custom() {
    NestedIfStatementsCheck check = new NestedIfStatementsCheck();
    check.max = 4;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/NestedIfStatementsCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(20).withMessage("Refactor this code to not nest more than 4 if/for/while/switch/try statements.")
      .next().atLine(24);
  }

}
