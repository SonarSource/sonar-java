/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class AssertionsInProductionCodeCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AssertionsInProductionCodeCheckSample.java"))
      .withCheck(new AssertionsInProductionCodeCheck())
      .verifyIssues();
  }

  @Test
  void test_specific_package_name() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInProductionCodeCheckSample.java"))
      .withCheck(new AssertionsInProductionCodeCheck())
      .verifyNoIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/AssertionsInProductionCodeCheckSample.java"))
      .withCheck(new AssertionsInProductionCodeCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_package_name_regex() {
    var productionPackageNames = List.of(
      "org.sonar.java.checks",
      "it.sonar.java.checks");
    var testPackageNames = List.of(
      "org.sonar.java.checks.test",
      "org.sonar.test.checks",
      "org.sonar.java.checks.junit",
      "assert.org.sonar.java",
      "org.sonar.java.it",
      "assert",
      "test",
      "junit");
    for (String packageName : productionPackageNames) {
      assertThat(AssertionsInProductionCodeCheck.TEST_PACKAGE_REGEX.matcher(packageName).find()).isFalse();
    }
    for (String packageName : testPackageNames) {
      assertThat(AssertionsInProductionCodeCheck.TEST_PACKAGE_REGEX.matcher(packageName).find()).isTrue();
    }
  }

}
