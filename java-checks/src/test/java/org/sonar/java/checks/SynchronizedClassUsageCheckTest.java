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
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class SynchronizedClassUsageCheckTest {

  private final SynchronizedClassUsageCheck check = new SynchronizedClassUsageCheck();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SynchronizedClassUsageCheck.java"), new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(5).withMessage("Replace the synchronized class \"Vector\" by an unsynchronized one such as \"ArrayList\" or \"LinkedList\".")
        .next().atLine(6)
        .next().atLine(7).withMessage("Replace the synchronized class \"Hashtable\" by an unsynchronized one such as \"HashMap\".")
        .next().atLine(8)
        .next().atLine(9)
        .next().atLine(12)
        .next().atLine(13).withMessage("Replace the synchronized class \"StringBuffer\" by an unsynchronized one such as \"StringBuilder\".")
        .next().atLine(14).withMessage("Replace the synchronized class \"Stack\" by an unsynchronized one such as \"Deque\".")
        .next().atLine(19)
        .next().atLine(20)
        .next().atLine(23)
        .next().atLine(25)
        .next().atLine(28)
        .next().atLine(33)
        .next().atLine(35)
        .next().atLine(43)
        .next().atLine(49)
        .next().atLine(51);
  }

}
