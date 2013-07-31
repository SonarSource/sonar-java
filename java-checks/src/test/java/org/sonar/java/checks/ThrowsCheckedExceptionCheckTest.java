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

public class ThrowsCheckedExceptionCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void test() {
    SourceFile file = BytecodeFixture.scan("ThrowsCheckedExceptionCheck", new ThrowsCheckedExceptionCheck());
    checkMessagesVerifier
        .verify(file.getCheckMessages())

        .next().atLine(29).withMessage("Remove the usage of the checked exception 'java.lang.Throwable'.")
        .next().atLine(32).withMessage("Remove the usage of the checked exception 'java.lang.Error'.")
        .next().atLine(35).withMessage("Remove the usage of the checked exception 'org.sonar.java.checks.targets.ThrowsCheckedExceptionCheck$MyException'.")
        .next().atLine(47).withMessage("Remove the usage of the checked exception 'java.io.IOException'.")
        .next().atLine(47).withMessage("Remove the usage of the checked exception 'org.sonar.java.checks.targets.ThrowsCheckedExceptionCheck$MyException'.");
  }

}
