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
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class UselessImportCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected_with_package() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/UselessImportCheck/WithPackage.java"),
      new VisitorsBridge(new UselessImportCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(8).withMessage("Remove this unused import 'a.b.c.NonCompliant'.")
      .next().atLine(9)
      .next().atLine(15).withMessage("Remove this unnecessary import: java.lang classes are always implicitly imported.")
      .next().atLine(16)
      .next().atLine(17).withMessage("Remove this duplicated import.")
      .next().atLine(21).withMessage("Remove this unnecessary import: same package classes are always implicitly imported.")
      .next().atLine(26)
      .noMore();
  }

  @Test
  public void detected_without_package() {
    JavaCheckVerifier.verify("src/test/files/checks/UselessImportCheck/WithoutPackage.java", new UselessImportCheck());
  }

}
