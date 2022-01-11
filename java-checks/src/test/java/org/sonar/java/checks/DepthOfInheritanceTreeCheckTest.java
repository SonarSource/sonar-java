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

import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testSourcesPath;

class DepthOfInheritanceTreeCheckTest {

  @Test
  void defaults() {
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/DepthOfInheritanceTreeCheckOk.java"))
      .withCheck(new DepthOfInheritanceTreeCheck())
      .verifyNoIssues();
  }

  @Test
  void max_level_is_2() {
    DepthOfInheritanceTreeCheck check = new DepthOfInheritanceTreeCheck();
    check.setMax(2);

    String filename = testSourcesPath("checks/DepthOfInheritanceTreeCheck.java");

    CheckVerifier.newVerifier()
      .onFile(filename)
      .withCheck(check)
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(filename)
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void max_level_is_2_and_filtered() {
    DepthOfInheritanceTreeCheck check = new DepthOfInheritanceTreeCheck();
    check.setMax(2);
    check.setFilteredClasses("java.lang.Object");

    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/DepthOfInheritanceTreeCheckFiltered.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void intermediate_match() {
    DepthOfInheritanceTreeCheck check = new DepthOfInheritanceTreeCheck();
    check.setMax(2);
    check.setFilteredClasses("checks.Dit_C");

    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/DepthOfInheritanceTreeCheckIntermediateMatching.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_framework_exclusion() {
    DepthOfInheritanceTreeCheck check = new DepthOfInheritanceTreeCheck();
    check.setMax(1);
    CheckVerifier.newVerifier()
      .onFile(testSourcesPath("checks/DepthOfInheritanceTreeCheckFrameworkExclusion.java"))
      .withCheck(check)
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/DepthOfInheritanceTreeCheckFrameworkExclusion.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

}
