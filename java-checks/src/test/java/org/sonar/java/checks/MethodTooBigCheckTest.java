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

import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class MethodTooBigCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    assertThat(new MethodTooBigCheck().max).isEqualTo(100);
  }

  @Test
  public void custom_at_4() {
    MethodTooBigCheck check = new MethodTooBigCheck();
    check.max = 4;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/MethodTooBigCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(2).withMessage("This method has 6 lines, which is greater than the 4 lines authorized. Split it into smaller methods.")
        .next().atLine(9).withMessage("This method has 5 lines, which is greater than the 4 lines authorized. Split it into smaller methods.");
  }

  @Test
  public void custom_at_5() {
    MethodTooBigCheck check = new MethodTooBigCheck();
    check.max = 5;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/MethodTooBigCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(2).withMessage("This method has 6 lines, which is greater than the 5 lines authorized. Split it into smaller methods.");
  }

}
