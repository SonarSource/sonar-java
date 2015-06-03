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

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

@SuppressWarnings("unchecked")
public class AvoidStarImportCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void allowAll() {
    AvoidStarImportCheck check = new AvoidStarImportCheck();
    check.allowClassImports = true;
    check.allowStaticMemberImports = true;
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages()).noMore();
  }

  @Test
  public void allowNoStatic() {
    AvoidStarImportCheck check = new AvoidStarImportCheck();
    check.allowClassImports = true;
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(5).withMessage("Using the '.*' form of import should be avoided - java.util.Arrays.*").noMore();
  }

  @Test
  public void allowStatic() {
    AvoidStarImportCheck check = new AvoidStarImportCheck();
    check.allowStaticMemberImports = true;
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(1).next().atLine(2).noMore();
  }

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(new AvoidStarImportCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(1).next().atLine(2).next().atLine(5).noMore();
  }

  @Test
  public void excludeNothing() {
    AvoidStarImportCheck check = new AvoidStarImportCheck();
    check.exclude = "java.util.noexist,java.io.noexist";
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(1).next().atLine(2).next().atLine(5).noMore();
  }

  @Test
  public void excludeNoStatic() {
    AvoidStarImportCheck check = new AvoidStarImportCheck();
    check.exclude = "java.util.noexist,java.io";
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(2).next().atLine(5).noMore();
  }

  @Test
  public void excludeStatic() {
    AvoidStarImportCheck check = new AvoidStarImportCheck();
    check.exclude = "java.util.Arrays";
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/AvoidStarImportCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(1).next().atLine(2).noMore();
  }

}
