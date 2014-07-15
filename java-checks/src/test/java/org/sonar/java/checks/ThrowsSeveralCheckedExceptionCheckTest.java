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

public class ThrowsSeveralCheckedExceptionCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  private final ThrowsSeveralCheckedExceptionCheck check = new ThrowsSeveralCheckedExceptionCheck();

  @Test
  public void test() {
    SourceFile file = BytecodeFixture.scan("ThrowsSeveralCheckedExceptionCheck", check);
    checkMessagesVerifier
      .verify(file.getCheckMessages())

      .next()
      .atLine(51)
      .withMessage(
        "Refactor this method to throw at most one checked exception instead of: java.io.IOException, org.sonar.java.checks.targets.ThrowsSeveralCheckedExceptionCheck$MyException")

      .next()
      .atLine(54)
      .withMessage("Refactor this method to throw at most one checked exception instead of: java.io.IOException, java.io.IOException, java.sql.SQLException")

      .next()
      .atLine(74)

      .next()
      .atLine(92)

      .next()
      .atLine(100);
  }

  @Test
  public void test_toString() {
    assertThat(check.toString()).isEqualTo("S1160 rule");
  }

}
