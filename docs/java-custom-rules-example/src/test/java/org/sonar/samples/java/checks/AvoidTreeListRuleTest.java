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
package org.sonar.samples.java.checks;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.samples.java.utils.FilesUtils;

class AvoidTreeListRuleTest {

  @Test
  void verify() {

    // Verifies automatically that the check will raise the adequate issues with the expected message
    CheckVerifier.newVerifier()
      .onFile("src/test/files/AvoidTreeListRule.java")
      .withCheck(new AvoidTreeListRule())
      // In order to test this check efficiently, we added the test-jar "org.apache.commons.commons-collections4" to the pom,
      // which is normally not used by the code of our custom plugin.
      // All the classes from this jar will then be read when verifying the ticket, allowing correct type resolution.
      // You have to give the test jar directory to the verifier in order to make it work correctly.
      .withClassPath(FilesUtils.getClassPath("target/test-jars"))
      .verifyIssues();
  }

}
