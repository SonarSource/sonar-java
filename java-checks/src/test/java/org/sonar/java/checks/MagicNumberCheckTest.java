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

import java.io.File;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.JavaCheckVerifier;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

public class MagicNumberCheckTest {

  @Test
  public void detected() {
    JavaCheckVerifier.verify("src/test/files/checks/MagicNumberCheck.java", new MagicNumberCheck());
  }

  @Test
  public void detectedWithTwoAuthorized() {
    // Use CheckMessagesVerifierRule to have only one test file
    CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();
    MagicNumberCheck check = new MagicNumberCheck();
    check.authorizedNumbers = "-1,0,1,2";

    SourceFile file = JavaAstScanner
      .scanSingleFile(new File("src/test/files/checks/MagicNumberCheck.java"), new VisitorsBridge(check));

    // Check first error at line 16 (=> 2 is authorized)
    checkMessagesVerifier.verify(file.getCheckMessages()).next().atLine(16);
  }
}
