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

import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class SunPackagesUsedCheckTest {

  private final SunPackagesUsedCheck check = new SunPackagesUsedCheck();

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SunPackagesUsedCheck.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(1).withMessage("Replace this usage of Sun classes by ones from the Java API.")
        .next().atLine(2)
        .next().atLine(7)
        .next().atLine(8)
        .next().atLine(10);
  }

  @Test
  public void check_with_exclusion() {
    check.exclude = "com.sun.imageio,com.sun.jersey";
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/SunPackagesUsedCheck.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(10).withMessage("Replace this usage of Sun classes by ones from the Java API.");
  }
}
