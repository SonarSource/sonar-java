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

import com.google.common.collect.Lists;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;

import java.io.File;

public class ParameterReassignedToCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/ParameterReassignedToCheck.java"), new VisitorsBridge(new ParameterReassignedToCheck(),
        Lists.newArrayList(new File("target/test-classes"))));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(6).withMessage("Introduce a new variable instead of reusing the parameter \"a\".")
      .next().atLine(7).withMessage("Introduce a new variable instead of reusing the parameter \"a\".")
      .next().atLine(12).withMessage("Introduce a new variable instead of reusing the parameter \"e\".")
      .next().atLine(28).withMessage("Introduce a new variable instead of reusing the parameter \"field\".")
      .next().atLine(32)
      .next().atLine(33)
      .next().atLine(34)
      .next().atLine(35);
  }

}
