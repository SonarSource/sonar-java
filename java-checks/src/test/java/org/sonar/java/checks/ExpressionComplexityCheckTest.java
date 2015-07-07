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

public class ExpressionComplexityCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ExpressionComplexityCheck.java"), new VisitorsBridge(new ExpressionComplexityCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(3).withMessage("Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).").withCost(1.0)
        .next().atLine(5).withMessage("Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).").withCost(1.0)
        .next().atLine(6).withMessage("Reduce the number of conditional operators (5) used in the expression (maximum allowed 3).").withCost(2.0)
        .next().atLine(11).withMessage("Reduce the number of conditional operators (6) used in the expression (maximum allowed 3).").withCost(3.0)
        .next().atLine(26).withMessage("Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).").withCost(1.0)
        .next().atLine(28).withMessage("Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).").withCost(1.0)
        .next().atLine(36)
        .next().atLine(45);
  }

  @Test
  public void custom() {
    ExpressionComplexityCheck check = new ExpressionComplexityCheck();
    check.max = 4;

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ExpressionComplexityCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(6).withMessage("Reduce the number of conditional operators (5) used in the expression (maximum allowed 4).").withCost(1.0)
        .next().atLine(11).withMessage("Reduce the number of conditional operators (6) used in the expression (maximum allowed 4).").withCost(2.0);
  }

}
