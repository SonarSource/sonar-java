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

public class ToStringUsingBoxingCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ToStringUsingBoxingCheck.java"), new ToStringUsingBoxingCheck());
    checkMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(3).withMessage("Call the static method Byte.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(4).withMessage("Call the static method Short.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(5).withMessage("Call the static method Integer.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(6).withMessage("Call the static method Long.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(7).withMessage("Call the static method Float.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(8).withMessage("Call the static method Double.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(9).withMessage("Call the static method Character.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(10).withMessage("Call the static method Boolean.toString(...) instead of instantiating a temporary object to perform this to string conversion.")
        .next().atLine(11).withMessage("Call the static method Integer.toString(...) instead of instantiating a temporary object to perform this to string conversion.");
  }

}
