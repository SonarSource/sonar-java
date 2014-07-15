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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class XPathCheckTest {

  private XPathCheck check = new XPathCheck();

  @Test
  public void test_JavaTokenType() {
    check.xpathQuery = "//IDENTIFIER[string-length(@tokenValue) >= 10]";
    check.message = "Avoid identifiers which are too long!";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/XPath.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(1).withMessage("Avoid identifiers which are too long!")
        .noMore();
  }

  @Test
  public void test_AstNodeType() {
    check.xpathQuery = "//classDeclaration";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/XPath.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(1)
        .noMore();
  }

  @Test
  public void test_JavaKeyword() {
    check.xpathQuery = "//PUBLIC";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/XPath.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(1)
        .noMore();
  }

  @Test
  public void test_JavaPunctuator() {
    check.xpathQuery = "//RWING";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/XPath.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(2)
        .noMore();
  }

  @Test
  public void parseError() {
    check.xpathQuery = "//IDENTIFIER";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ParsingError.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .noMore();
  }

}
