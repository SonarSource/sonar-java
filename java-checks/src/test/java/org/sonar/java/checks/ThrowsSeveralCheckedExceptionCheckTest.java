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

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class ThrowsSeveralCheckedExceptionCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  private final ThrowsSeveralCheckedExceptionCheck check = new ThrowsSeveralCheckedExceptionCheck();

  @Test
  public void test() {
    File bytecodeDir = new File("target/test-classes");
    assertThat(bytecodeDir).exists();
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/java/org/sonar/java/checks/targets/ThrowsSeveralCheckedExceptionCheck.java"), new VisitorsBridge(check, Lists.newArrayList(bytecodeDir)));
    checkMessagesVerifier
      .verify(file.getCheckMessages())

      .next().atLine(53)
      .withMessage(
          "Refactor this method to throw at most one checked exception instead of: java.io.IOException, ThrowsSeveralCheckedExceptionCheck.MyException")

      .next().atLine(56)
      .withMessage("Refactor this method to throw at most one checked exception instead of: java.io.IOException, java.io.IOException, java.sql.SQLException")
      .next().atLine(76)
      .next().atLine(94)
      .next().atLine(102)
      .next().atLine(107)
      .next().atLine(116)
      .next().atLine(117)
      .next().atLine(118)
      .next().atLine(121)
      .next().atLine(124)
      .next().atLine(127)
      .next().atLine(134)
      .next().atLine(140)
      .next().atLine(147)
      .next().atLine(153)
      .next().atLine(157)
      .next().atLine(162)
    ;
  }

}
