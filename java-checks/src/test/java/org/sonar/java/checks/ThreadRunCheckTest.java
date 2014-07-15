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
import org.sonar.squidbridge.api.SourceFile;

import static org.fest.assertions.Assertions.assertThat;

public class ThreadRunCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  private final ThreadRunCheck check = new ThreadRunCheck();

  @Test
  public void detected() {
    SourceFile file = BytecodeFixture.scan("ThreadRunCheck", check);
    checkMessagesVerifier
      .verify(file.getCheckMessages())
      .next().atLine(29).withMessage("Call the method Thread.start() to execute the content of the run() method in a dedicated thread.")
      .next().atLine(39)
      .next().atLine(42)
      .next().atLine(45)
      .next().atLine(53);
  }

  @Test
  public void test_toString() {
    assertThat(check.toString()).isEqualTo("S1217 rule");
  }

}
