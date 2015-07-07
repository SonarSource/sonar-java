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

public class MismatchPackageDirectoryCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void correctMatch() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/mismatchPackage/Matching.java"), new VisitorsBridge(new MismatchPackageDirectoryCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages()).noMore();
  }
  @Test
  public void defaultPackage() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/mismatchPackage/DefaultPackage.java"), new VisitorsBridge(new MismatchPackageDirectoryCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages()).noMore();
  }
  @Test
  public void mismatch() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/mismatchPackage/Mismatch.java"), new VisitorsBridge(new MismatchPackageDirectoryCheck()));
    String expectedLocation = "org"+File.separator+"foo"+File.separator+"mismatchPackage";
    String actualLocation = "src"+File.separator+"test"+File.separator+"files"+File.separator+"checks"+File.separator+"mismatchPackage";
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(1).withMessage("This file \"Mismatch.java\" should be located in \""+expectedLocation+"\" directory, not in \""+actualLocation+"\".");
  }

}
