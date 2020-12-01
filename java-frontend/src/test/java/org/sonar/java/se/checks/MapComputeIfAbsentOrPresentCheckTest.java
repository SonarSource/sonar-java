/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.se.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.se.SETestUtils;
import org.sonar.java.testing.CheckVerifier;

import static org.sonar.java.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.TestUtils.testSourcesPath;

class MapComputeIfAbsentOrPresentCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("symbolicexecution/checks/MapComputeIfAbsentOrPresentCheck.java"))
      .withCheck(new MapComputeIfAbsentOrPresentCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("symbolicexecution/checks/MapComputeIfAbsentOrPresentCheck.java"))
      .withCheck(new MapComputeIfAbsentOrPresentCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }
}
