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

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.squidbridge.checks.CheckMessagesVerifierRule;

import java.io.File;

public class DeprecatedHashAlgorithmCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/DeprecatedHashAlgorithmCheck.java"),
      new VisitorsBridge(new DeprecatedHashAlgorithmCheck(), ImmutableList.of(new File("target/test-classes"))));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(8).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(9).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(11).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(12).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(14).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(15).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(16).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(18).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(19).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(20).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(21).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(22).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(23).withMessage("Use a stronger encryption algorithm than SHA-1.")
      .next().atLine(28).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(29).withMessage("Use a stronger encryption algorithm than MD5.")
      .next().atLine(30).withMessage("Use a stronger encryption algorithm than SHA-1.");
  }

}
