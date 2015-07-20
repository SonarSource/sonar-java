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

public class IdenticalOperandOnBinaryExpressionCheckTest {

  private IdenticalOperandOnBinaryExpressionCheck check = new IdenticalOperandOnBinaryExpressionCheck();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/IdenticalOperandOnBinaryExpression.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(5).withMessage("Identical sub-expressions on both sides of operator \"==\"")
        .next().atLine(6).withMessage("Identical sub-expressions on both sides of operator \"!=\"")
        .next().atLine(7).withMessage("Identical sub-expressions on both sides of operator \"||\"")
        .next().atLine(8).withMessage("Identical sub-expressions on both sides of operator \"&&\"")
        .next().atLine(9).withMessage("Identical sub-expressions on both sides of operator \"||\"")
        .next().atLine(10).withMessage("Identical sub-expressions on both sides of operator \"||\"")
        .next().atLine(11).withMessage("Identical sub-expressions on both sides of operator \"||\"")
        .next().atLine(13).withMessage("Identical sub-expressions on both sides of operator \"&&\"")
        .next().atLine(16).withMessage("Identical sub-expressions on both sides of operator \"||\"")
        .next().atLine(25).withMessage("Identical sub-expressions on both sides of operator \"-\"")
        .next().atLine(26).withMessage("Identical sub-expressions on both sides of operator \"-\"")
        .noMore();
  }
}
