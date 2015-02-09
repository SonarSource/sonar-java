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

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class PrimitiveTypeBoxingWithToStringCheckTest {

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/PrimitiveTypeBoxingWithToStringCheck.java"), new VisitorsBridge(
      new PrimitiveTypeBoxingWithToStringCheck(),
      Lists.newArrayList(new File("target/test-classes"))));
    new CheckMessagesVerifierRule().verify(file.getCheckMessages())
      .next().atLine(6).withMessage("Use \"Integer.toString\" instead.")
      .next().atLine(9).withMessage("Use \"Integer.toString\" instead.")
      .next().atLine(10).withMessage("Use \"Integer.toString\" instead.")
      .next().atLine(12).withMessage("Use \"Boolean.toString\" instead.")
      .next().atLine(14).withMessage("Use \"Boolean.toString\" instead.")
      .next().atLine(19).withMessage("Use \"Byte.toString\" instead.")
      .next().atLine(20).withMessage("Use \"Character.toString\" instead.")
      .next().atLine(21).withMessage("Use \"Short.toString\" instead.")
      .next().atLine(22).withMessage("Use \"Long.toString\" instead.")
      .next().atLine(23).withMessage("Use \"Float.toString\" instead.")
      .next().atLine(24).withMessage("Use \"Double.toString\" instead.")
      .noMore();
  }

}
