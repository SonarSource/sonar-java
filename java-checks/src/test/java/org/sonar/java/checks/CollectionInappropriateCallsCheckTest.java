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

public class CollectionInappropriateCallsCheckTest {

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/CollectionInappropriateCallsCheck.java"), new VisitorsBridge(
      new CollectionInappropriateCallsCheck()));
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(25).withMessage("A \"List<String>\" cannot contain a \"Integer\"")
      .next().atLine(26).withMessage("A \"List<String>\" cannot contain a \"Integer\"")
      .next().atLine(28).withMessage("A \"ArrayList<B>\" cannot contain a \"Integer\"")
      .next().atLine(29).withMessage("A \"List<Set>\" cannot contain a \"String\"")
      .next().atLine(30).withMessage("A \"List<Set>\" cannot contain a \"Integer\"")
      .next().atLine(31).withMessage("A \"List<Set>\" cannot contain a \"Integer\"")
      // .next().atLine(34).withMessage("A \"List<String>\" cannot contain a \"String[]\"") // False negative
      .next().atLine(35).withMessage("A \"List<String>\" cannot contain a \"Integer\"")
      .next().atLine(66).withMessage("A \"List<Integer>\" cannot contain a \"long\"")
      .next().atLine(69).withMessage("A \"List<String>\" cannot contain a \"int\"")
      .next().atLine(73) // False positive
      .noMore();
  }
}
