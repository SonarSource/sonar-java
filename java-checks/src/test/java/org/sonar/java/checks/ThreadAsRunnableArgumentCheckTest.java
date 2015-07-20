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

public class ThreadAsRunnableArgumentCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/ThreadAsRunnableArgumentCheck.java"),
      new VisitorsBridge(new ThreadAsRunnableArgumentCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(9).withMessage("\"t\" is a \"Thread\".")
      .next().atLine(11).withMessage("\"Argument 1\" is a \"Thread\".")
      .next().atLine(14).withMessage("\"myThread\" is a \"Thread\".")
      .next().atLine(25).withMessage("\"myThread\" is a \"Thread\".")
      .next().atLine(26).withMessage("\"myThread\" is a \"Thread\".")
      .next().atLine(27).withMessage("\"Argument 2\" is a \"Thread\".")
      .next().atLine(28).withMessage("\"Argument 4\" is a \"Thread\".")
      .next().atLine(28).withMessage("\"myThread\" is a \"Thread\".")
      .next().atLine(29).withMessage("\"Argument 2\" is a \"Thread[]\".")
      .noMore();
  }
}
