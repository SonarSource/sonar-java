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

public class OSCommandInjectionCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/OSCommandInjectionCheck.java"), new VisitorsBridge(new OSCommandInjectionCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(5).withMessage("Make sure \"input\" is properly sanitized before use in this OS command.")
        .next().atLine(6).withMessage("Make sure \"cmds\" is properly sanitized before use in this OS command.")
        .next().atLine(7).withMessage("Make sure \"input\" is properly sanitized before use in this OS command.")
        .next().atLine(12).withMessage("Make sure \"argument\" is properly sanitized before use in this OS command.")
        .next().atLine(12).withMessage("Make sure \"command\" is properly sanitized before use in this OS command.")
        .next().atLine(13).withMessage("Make sure \"argument\" is properly sanitized before use in this OS command.")
        .next().atLine(13).withMessage("Make sure \"command\" is properly sanitized before use in this OS command.")
        .next().atLine(14).withMessage("Make sure \"argument\" is properly sanitized before use in this OS command.")
        .next().atLine(15).withMessage("Make sure \"argument\" is properly sanitized before use in this OS command.")
        .next().atLine(15).withMessage("Make sure \"command\" is properly sanitized before use in this OS command.")
    ;
  }
}
