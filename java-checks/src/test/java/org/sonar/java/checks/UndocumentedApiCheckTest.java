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

import com.sonar.sslr.squid.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.squid.api.SourceFile;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class UndocumentedApiCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    assertThat(check.forClasses).isEqualTo("**");

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/UndocumentedApi.java"), check);
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
        .next().atLine(68).withMessage("Document this '<T>' parameter.")
        .next().atLine(77)
        .next().atLine(101).withMessage("Document this 'value' parameter.")
        .next().atLine(107).withMessage("Document this method return value.")
        .next().atLine(121).withMessage("Document this method return value.")
        .next().atLine(130).withMessage("Document this 'a' parameter.")
        .next().atLine(139).withMessage("Document this 'a' parameter.");
  }

  @Test
  public void custom() {
    UndocumentedApiCheck check = new UndocumentedApiCheck();
    check.forClasses = "";

    SourceFile file = JavaAstScanner.scanSingleFile(new File("src/test/files/checks/UndocumentedApi.java"), check);
    checkMessagesVerifier.verify(file.getCheckMessages());
  }

}
