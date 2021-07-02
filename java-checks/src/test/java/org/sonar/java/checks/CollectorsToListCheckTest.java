/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class CollectorsToListCheckTest {
  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/CollectorsToList.java"))
      .withCheck(new CollectorsToListCheck())
      .withJavaVersion(16)
      .verifyIssues();
  }

  @Test
  void test_no_issues_for_older_versions() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/CollectorsToList.java"))
      .withCheck(new CollectorsToListCheck())
      .withJavaVersion(15)
      .verifyNoIssues();
  }

  @Test
  void test_no_issues_for_unknown_versions() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/CollectorsToList.java"))
      .withCheck(new CollectorsToListCheck())
      .verifyNoIssues();
  }
}
