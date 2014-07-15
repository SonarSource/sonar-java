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

import org.sonar.squidbridge.checks.CheckMessagesVerifier;
import org.junit.Test;
import org.sonar.squidbridge.api.SourceFile;

import static org.fest.assertions.Assertions.assertThat;

public class ArchitectureCheckTest {

  private final ArchitectureCheck check = new ArchitectureCheck();

  @Test
  public void test() {
    check.setToClasses("java.**.Pattern");
    SourceFile file = BytecodeFixture.scan("ArchitectureConstraint", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(27).withMessage("org/sonar/java/checks/targets/ArchitectureConstraint must not use java/util/regex/Pattern")
      .noMore();
  }

  @Test
  public void test2() {
    check.setFromClasses("com.**");
    check.setToClasses("java.**.Pattern");
    SourceFile file = BytecodeFixture.scan("ArchitectureConstraint", check);
    CheckMessagesVerifier.verify(file.getCheckMessages())
      .noMore();
  }

  @Test
  public void test_toString() {
    assertThat(check.toString()).isEqualTo("ArchitecturalConstraint rule");
  }

}
