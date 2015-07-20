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

public class WaitInSynchronizeCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/WaitInSynchronizeCheck.java"), new VisitorsBridge(new WaitInSynchronizeCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(20).withMessage("Make this call to \"wait()\" only inside a synchronized block to be sure to hold the monitor on \"String\" object.")
        .next().atLine(21).withMessage("Make this call to \"wait()\" only inside a synchronized block to be sure to hold the monitor on \"this\" object.")
        .next().atLine(22).withMessage("Make this call to \"wait()\" only inside a synchronized block to be sure to hold the monitor on \"this\" object.")
        .next().atLine(23).withMessage("Make this call to \"notify()\" only inside a synchronized block to be sure to hold the monitor on \"this\" object.")
        .next().atLine(24).withMessage("Make this call to \"notifyAll()\" only inside a synchronized block to be sure to hold the monitor on \"this\" object.")
        .next().atLine(31).withMessage("Make this call to \"wait()\" only inside a synchronized block to be sure to hold the monitor on \"this\" object.")
    .noMore();
  }
}
