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

public class SunPackagesUsedCheckTest {

  private final SunPackagesUsedCheck check = new SunPackagesUsedCheck();

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SunPackagesUsedCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(1).withMessage("Replace this usage of Sun classes by ones from the Java API.")
      .next().atLine(2)
      .next().atLine(7)
      .next().atLine(8)
      .next().atLine(10)
      .next().atLine(11)
      .next().atLine(13)
      .next().atLine(17);
  }

  @Test
  public void check_with_exclusion() {
    check.exclude = "com.sun.imageio,com.sun.jersey,com.sun.org.apache.xml";
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SunPackagesUsedCheck.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(10).withMessage("Replace this usage of Sun classes by ones from the Java API.")
      .next().atLine(13).withMessage("Replace this usage of Sun classes by ones from the Java API.")
      .next().atLine(17);
  }

}
