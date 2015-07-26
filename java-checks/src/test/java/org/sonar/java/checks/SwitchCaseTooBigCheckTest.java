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

public class SwitchCaseTooBigCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SwitchCaseTooBigCheck.java"), new VisitorsBridge(new SwitchCaseTooBigCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(10).withMessage("Reduce this switch case number of lines from 7 to at most 5, for example by extracting code into methods.")
      .next().atLine(18).withMessage("Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.")
      .next().atLine(24).withMessage("Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.")
      .next().atLine(32).withMessage("Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.")
      .next().atLine(38).withMessage("Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.")
      .next().atLine(44).withMessage("Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.")
      .next().atLine(51).withMessage("Reduce this switch case number of lines from 6 to at most 5, for example by extracting code into methods.")
      .noMore();
  }

  @Test
  public void custom() {
    SwitchCaseTooBigCheck check = new SwitchCaseTooBigCheck();
    check.max = 6;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SwitchCaseTooBigCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(10).withMessage("Reduce this switch case number of lines from 7 to at most 6, for example by extracting code into methods.")
      .noMore();
  }

  @Test
  public void limit() {
    SwitchCaseTooBigCheck check = new SwitchCaseTooBigCheck();
    check.max = 0;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SwitchCaseTooBigCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(4)
      .next().atLine(10)
      .next().atLine(18)
      .next().atLine(24)
      .next().atLine(31)
      .next().atLine(32)
      .next().atLine(38)
      .next().atLine(44)
      .next().atLine(51)
      .next().atLine(61)
      .next().atLine(66)
      .next().atLine(74)
      .next().atLine(74)
      .next().atLine(77)
      .next().atLine(83)
      .noMore();
  }

}
