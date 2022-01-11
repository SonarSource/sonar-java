/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

class UppercaseSuffixesCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UppercaseSuffixesCheck.java")
      .withCheck(new UppercaseSuffixesCheck())
      .verifyIssues();
  }

  @Test
  void test_only_long() {
    UppercaseSuffixesCheck check = new UppercaseSuffixesCheck();
    check.checkOnlyLong = true;
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/UppercaseSuffixesCheckOnlyLong.java")
      .withCheck(check)
      .verifyIssues();
  }

}
