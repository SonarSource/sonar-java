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

import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifier;

import java.io.File;

public class CloseResourceCheckTest {

  private final CloseResourceCheck check = new CloseResourceCheck();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/CloseResourceCheck.java"),
      new VisitorsBridge(check));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(25).withMessage("Close this \"Reader\"")
      .next().atLine(27).withMessage("Close this \"Writer\"")
      .next().atLine(33).withMessage("Close this \"InputStream\"")
      .next().atLine(35).withMessage("Close this \"RandomAccessFile\"")
      .next().atLine(82).withMessage("Close this \"Reader\"")
      .next().atLine(87).withMessage("Close this \"Reader\"")
      .next().atLine(93).withMessage("Close this \"Writer\"")
      .next().atLine(98).withMessage("Close this \"Formatter\"")
      .next().atLine(104).withMessage("Close this \"BufferedWriter\"")
      .next().atLine(111).withMessage("Close this \"FileInputStream\"")
      .next().atLine(184).withMessage("Close this \"InputStream\"")
      .noMore();
  }
}
