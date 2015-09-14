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

public class ClassCouplingCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ClassCouplingCheck.java"), new VisitorsBridge(new ClassCouplingCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1)
      .withMessage("Split this class into smaller and more specialized ones to reduce its dependencies on other classes from 21 to the maximum authorized 20 or less.")
      .next().atLine(33)
      .next().atLine(60)
      .next().atLine(85)
      .next().atLine(144)
      .withMessage("Split this class into smaller and more specialized ones to reduce its dependencies on other classes from 21 to the maximum authorized 20 or less.")
      .next().atLine(167)
      .noMore();
  }

  @Test
  public void custom() {
    ClassCouplingCheck check = new ClassCouplingCheck();
    check.max = 22;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ClassCouplingCheck.java"), new VisitorsBridge(check));

    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(33)
      .withMessage("Split this class into smaller and more specialized ones to reduce its dependencies on other classes from 23 to the maximum authorized 22 or less.");
  }

}
