/*
 * Sonar Java
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

import com.sonar.sslr.squid.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.squid.api.SourceFile;

public class LCOM4CheckTest {

  private LCOM4Check check = new LCOM4Check();

  @Test
  public void defaults() {
    SourceFile file = BytecodeFixture.scan("Lcom4", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .next().atLine(22).withMessage("This class has an LCOM4 of 2, which is greater than 1 authorized.")
        .noMore();
  }

  @Test
  public void test() {
    check.setMax(2);
    SourceFile file = BytecodeFixture.scan("Lcom4", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
        .noMore();
  }

}
