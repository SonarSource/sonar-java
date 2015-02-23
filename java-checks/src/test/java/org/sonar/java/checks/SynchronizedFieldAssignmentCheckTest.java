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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class SynchronizedFieldAssignmentCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SynchronizedFieldAssignmentCheck.java"),
      new VisitorsBridge(new SynchronizedFieldAssignmentCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(9).withMessage("Don't synchronize on \"color\" or remove its reassignment on line 10.")
      .next().atLine(22).withMessage("Don't synchronize on \"b\" or remove its reassignment on line 23.")
      .next().atLine(26).withMessage("Don't synchronize on \"color\" or remove its reassignment on line 27.")
      .next().atLine(30).withMessage("Don't synchronize on \"color\" or remove its reassignment on line 31.")
      .next().atLine(34).withMessage("Don't synchronize on \"val\" or remove its reassignment on line 35.")
      .next().atLine(38).withMessage("Don't synchronize on \"val\" or remove its reassignment on line 39.")
      .next().atLine(42).withMessage("Don't synchronize on \"val\" or remove its reassignment on line 43.")
      .next().atLine(46).withMessage("Don't synchronize on \"val\" or remove its reassignment on line 47.")
      .noMore();
  }
}
