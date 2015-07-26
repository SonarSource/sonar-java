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

public class ReturnEmptyArrayNotNullCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/ReturnEmptyArrayNotNullCheck.java"), new VisitorsBridge(new ReturnEmptyArrayNotNullCheck()));
//      new File("/home/benzonico/Development/SonarSource/temp/lucene/lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java"), new VisitorsBridge(new ReturnEmptyArrayNotNullCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(29).withMessage("Return an empty array instead of null.")
      .next().atLine(38)
      .next().atLine(48)
      .next().atLine(53)
      .next().atLine(57)
      .next().atLine(61)
      .next().atLine(66).withMessage("Return an empty collection instead of null.")
      .next().atLine(70).withMessage("Return an empty array instead of null.")
      .next().atLine(74)
      .next().atLine(84)
      .next().atLine(92)
      .next().atLine(100)
      .next().atLine(111)
      .noMore();
  }

}
