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
import org.sonar.squid.api.SourceFile;

public class HiddenFieldCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    HiddenFieldCheck check = new HiddenFieldCheck();

    SourceFile file = BytecodeFixture.scan("HiddenFieldCheck", check);
    checkMessagesVerifier
        .verify(file.getCheckMessages())
        .next().atLine(28).withMessage("Rename this variable/parameter which hides the field declared at line 24.")
        .next().atLine(50).withMessage("Rename this variable/parameter which hides the field declared at line 24.")
        .next().atLine(63).withMessage("Rename this variable/parameter which hides the field declared at line 25.")
        .next().atLine(80).withMessage("Rename this variable/parameter which hides the field declared at line 76.")
        .next().atLine(87).withMessage("Rename this variable/parameter which hides the field declared at line 24.")
        .next().atLine(88)
        .next().atLine(108)
        .next().atLine(109)
        .next().atLine(140);
  }

}
