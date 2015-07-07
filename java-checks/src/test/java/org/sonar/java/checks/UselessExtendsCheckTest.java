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

public class UselessExtendsCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/UselessExtendsCheck.java"),
      new VisitorsBridge(new UselessExtendsCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(7).withMessage("\"Object\" should not be explicitly extended.")
      .next().atLine(12).withMessage("\"I1\" is listed multiple times.")
      .next().atLine(16).withMessage("\"I3\" is a \"I1\" so \"I1\" can be removed from the extension list.")
      .next().atLine(17).withMessage("\"I3\" is a \"I2\" so \"I2\" can be removed from the extension list.")
      .next().atLine(24).withMessage("\"Object\" should not be explicitly extended.")
      .next().atLine(31).withMessage("\"UnknownInterface\" is listed multiple times.")
      .next().atLine(32).withMessage("\"UnknownInterface\" is listed multiple times.")
      .next().atLine(33).withMessage("\"UnknownInterface\" is listed multiple times.")
      .next().atLine(34).withMessage("\"UnknownInterface\" is listed multiple times.")
      .next().atLine(35).withMessage("\"UnknownParametrized\" is listed multiple times.")
      .next().atLine(36).withMessage("\"UnknownParametrized\" is listed multiple times.");
  }

}
