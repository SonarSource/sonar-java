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

public class UnusedMethodParameterCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/UnusedMethodParameterCheck.java"), new VisitorsBridge(new UnusedMethodParameterCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(2).withMessage("Remove the unused method parameter(s) \"b\".")
      .next().atLine(37).withMessage("Remove the unused method parameter(s) \"b,a\".")
      .next().atLine(42).withMessage("Remove the unused method parameter(s) \"a\".")
      .next().atLine(50).withMessage("Remove the unused method parameter(s) \"a\".")
      .next().atLine(55).withMessage("Remove the unused method parameter(s) \"args\".")
      .next().atLine(56).withMessage("Remove the unused method parameter(s) \"args\".")
      .next().atLine(57).withMessage("Remove the unused method parameter(s) \"args\".")
      .next().atLine(58).withMessage("Remove the unused method parameter(s) \"args\".")
      .next().atLine(59).withMessage("Remove the unused method parameter(s) \"args\".")
      .next().atLine(88).withMessage("Remove the unused method parameter(s) \"arg\".")
    .noMore();
  }

}
