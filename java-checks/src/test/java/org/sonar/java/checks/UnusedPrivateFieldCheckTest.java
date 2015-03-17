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

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class UnusedPrivateFieldCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = scanFile("UnusedPrivateFieldCheck.java");
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(3).withMessage("Remove this unused \"unusedField\" private field.")
      .next().atLine(6).withMessage("Remove this unused \"foo\" private field.")
      .next().atLine(15).withMessage("Remove this unused \"unreadField\" private field.")
      .next().atLine(21).withMessage("Remove this unused \"innerClassUnreadField\" private field.");
  }

  @Test
  public void testNative() {
    SourceFile file = scanFile("UnusedPrivateFieldCheckWithNative.java");
    checkMessagesVerifier.verify(file.getCheckMessages()).noMore();
  }

  private SourceFile scanFile(String fileName) {
    return JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/" + fileName),
      new VisitorsBridge(new UnusedPrivateFieldCheck(), ImmutableList.of(new File("target/test-classes"))));
  }

  @Test
  public void testLombok() {
    for (String name : new File("src/test/files/checks/lombok/").list()) {
      SourceFile file = scanFile("lombok/" + name);
      checkMessagesVerifier.verify(file.getCheckMessages()).noMore();
    }
  }
}
