/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class UselessConditionCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/UselessConditionCheck.java"),
      new VisitorsBridge(new UselessConditionCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(26).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(28).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(30).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(32).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(34).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(38).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(47).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(49).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(53).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(55).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(57).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(61).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(77).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(80).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(90).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(92).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(109).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(111).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(130).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(132).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(142).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(146).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(163).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(172).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(178).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(180).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(193).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(206).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(208).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(210).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(228).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(230).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(232).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(234).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(236).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(238).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(240).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(242).withMessage("Change this condition so that it does not always evaluate to \"true\"")
      .next().atLine(244).withMessage("Change this condition so that it does not always evaluate to \"false\"")
      .next().atLine(246).withMessage("Change this condition so that it does not always evaluate to \"true\"");
  }

}
