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

import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;
import java.text.MessageFormat;

public class ShiftOnIntOrLongCheckTest {

  private static final String USELESS_SHIFT = "Remove this useless shift";

  private String getMessage(String identifier, String value) {
    return MessageFormat.format("Either make \"{0}\" a \"long\" or correct this shift to {1}", identifier, value);
  }

  private String getMessage(boolean isInt, String value) {
    return isInt ? MessageFormat.format("Either use a \"long\" or correct this shift to {0}", value) : MessageFormat.format("Correct this shift to {0}", value);
  }

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ShiftOnIntOrLongCheck.java"), new VisitorsBridge(new ShiftOnIntOrLongCheck()));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(6).withMessage(USELESS_SHIFT)
      .next().atLine(7).withMessage(USELESS_SHIFT)
      .next().atLine(8).withMessage(getMessage("a", "1"))
      .next().atLine(9).withMessage(getMessage("a", "-1"))
      .next().atLine(13).withMessage(USELESS_SHIFT)
      .next().atLine(14).withMessage(USELESS_SHIFT)
      .next().atLine(15).withMessage(getMessage("a", "1"))
      .next().atLine(16).withMessage(getMessage("a", "-1"))
      .next().atLine(17).withMessage(getMessage("b", "16"))

      .next().atLine(24).withMessage(USELESS_SHIFT)
      .next().atLine(25).withMessage(USELESS_SHIFT)
      .next().atLine(26).withMessage(getMessage(false, "1"))
      .next().atLine(27).withMessage(getMessage(false, "-1"))
      .next().atLine(31).withMessage(USELESS_SHIFT)
      .next().atLine(32).withMessage(USELESS_SHIFT)
      .next().atLine(33).withMessage(getMessage(false, "1"))
      .next().atLine(34).withMessage(getMessage(false, "-1"))
      .next().atLine(35).withMessage(getMessage(false, "32"))

      .next().atLine(47).withMessage(USELESS_SHIFT)
      .next().atLine(48).withMessage(getMessage(false, "32"))
      .next().atLine(49).withMessage(getMessage(false, "33"))
      .next().atLine(50).withMessage(getMessage(false, "34"))
      .next().atLine(51).withMessage(getMessage(false, "35"))
      .next().atLine(53).withMessage(USELESS_SHIFT)

      .next().atLine(66).withMessage(USELESS_SHIFT)
      .next().atLine(67).withMessage(getMessage(true, "16"))
      .next().atLine(68).withMessage(getMessage(true, "17"))
      .next().atLine(69).withMessage(getMessage("d", "18"))
      .next().atLine(70).withMessage(getMessage("e", "19"))
      .next().atLine(72).withMessage(USELESS_SHIFT)
      .noMore();
  }
}
