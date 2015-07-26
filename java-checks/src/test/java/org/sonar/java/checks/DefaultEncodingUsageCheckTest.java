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
import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class DefaultEncodingUsageCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/DefaultEncodingUsageCheck.java"),
      new VisitorsBridge(new DefaultEncodingUsageCheck()));
    CheckMessagesVerifier verifier = checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(15).withMessage("Remove this use of constructor \"String(byte[])\"")
      .next().atLine(16).withMessage("Remove this use of constructor \"String(byte[],int,int)\"")
      .next().atLine(17).withMessage("Remove this use of \"getBytes\"")
      .next().atLine(18).withMessage("Remove this use of \"getBytes\"");
    for (int i = 19; i <= 47; i++) {
        verifier.next().atLine(i);
      }
  }

}
