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
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class FileHeaderCheckTest {

  @Test
  public void test() {
    FileHeaderCheck check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class1.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 20\\d\\d";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class1.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(null);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2005";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(null).withMessage("Add or update the header of this file.");

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\n// foo";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r// foo";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\r\r// foo";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(null);

    check = new FileHeaderCheck();
    check.headerFormat = "// copyright 2012\n// foo\n\n\n\n\n\n\n\n\n\ngfoo";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class2.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(null);

    check = new FileHeaderCheck();
    check.headerFormat = "/*foo http://www.example.org*/";

    file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/FileHeaderCheck/Class3.java"), check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();
  }

}
