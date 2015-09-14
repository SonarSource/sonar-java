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

public class DefaultPackageCheckTest {

  @Rule
  public final CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  private final DefaultPackageCheck check = new DefaultPackageCheck();

  @Test
  public void without_package() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/DefaultPackageCheck/WithoutPackage.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().withMessage("Move this file to a named package.");
  }

  @Test
  public void with_package() {
    SourceFile file = JavaAstScanner.scanSingleFile(
        new File("src/test/files/checks/DefaultPackageCheck/WithPackage.java"),
        new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages());
  }

}
