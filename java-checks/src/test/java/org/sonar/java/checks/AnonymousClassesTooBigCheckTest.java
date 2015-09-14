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

public class AnonymousClassesTooBigCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AnonymousClassesTooBigCheck.java"), new VisitorsBridge(new AnonymousClassesTooBigCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(20).withMessage("Reduce this anonymous class number of lines from 21 to at most 20, or make it a named class.");
  }

  @Test
  public void custom() {
    AnonymousClassesTooBigCheck check = new AnonymousClassesTooBigCheck();
    check.max = 6;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AnonymousClassesTooBigCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(12).withMessage("Reduce this anonymous class number of lines from 7 to at most 6, or make it a named class.")
      .next().atLine(20).withMessage("Reduce this anonymous class number of lines from 21 to at most 6, or make it a named class.")
      .next().atLine(45).withMessage("Reduce this lambda expression number of lines from 8 to at most 6.")
      .next().atLine(55).withMessage("Reduce this lambda expression number of lines from 7 to at most 6.");
  }

}
