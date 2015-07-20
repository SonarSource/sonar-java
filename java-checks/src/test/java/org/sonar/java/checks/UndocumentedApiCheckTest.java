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

import static org.fest.assertions.Assertions.assertThat;

public class UndocumentedApiCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    assertThat(check.forClasses).isEqualTo("**");

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/UndocumentedApi.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(5)
      .next().atLine(11)
      .next().atLine(15)
      .next().atLine(18)
      .next().atLine(46).withMessage("Document this public enum.")
      .next().atLine(49).withMessage("Document this public interface.")
      .next().atLine(52).withMessage("Document this public annotation.")
      .next().atLine(55).withMessage("Document this public class.")
      .next().atLine(57).withMessage("Document this public field.")
      .next().atLine(59).withMessage("Document this public constructor.")
      .next().atLine(68).withMessage("Document the parameter(s): <T>")
      .next().atLine(77)
      .next().atLine(101).withMessage("Document the parameter(s): value")
      .next().atLine(107).withMessage("Document this method return value.")
      .next().atLine(121).withMessage("Document this method return value.")
      .next().atLine(130).withMessage("Document the parameter(s): a")
      .next().atLine(139).withMessage("Document the parameter(s): a")
      .next().atLine(162).withMessage("Document the parameter(s): a, b, c")
      .next().atLine(162).withMessage("Document this method return value.")
      .next().atLine(167).withMessage("Document this public method.")
      .next().atLine(177).withMessage("Document this public method.")
      .next().atLine(183).withMessage("Document this method return value.")
      .next().atLine(208).withMessage("Document this public class.")
      .next().atLine(262)
      .next().atLine(271)
      .next().atLine(277)
      .next().atLine(278);
  }

  @Test
  public void custom() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/UndocumentedApi.java"), new VisitorsBridge(check));
    checkMessagesVerifier.verify(file.getCheckMessages());
  }

}
