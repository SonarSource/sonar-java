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
package org.sonar.java.checks.tests;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@EnableRuleMigrationSupport
class AssertionsInTestsCheckTest {

  private AssertionsInTestsCheck check = new AssertionsInTestsCheck();

  @Rule
  public LogTester logTester = new LogTester();

  @BeforeEach
  void setup() {
    check.customAssertionMethods = "org.sonarsource.helper.AssertionsHelper$ConstructorAssertion#<init>,org.sonarsource.helper.AssertionsHelper#customAssertionAsRule*," +
      "org.sonarsource.helper.AssertionsHelper#customInstanceAssertionAsRuleParameter,blabla,bla# , #bla";
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Junit3",
    "Junit4",
    "Junit5",
    "AssertJ",
    "Hamcrest",
    "Spring",
    "EasyMock",
    "Truth",
    "ReactiveX1",
    "ReactiveX2",
    "RestAssured",
    "Mockito",
    "JMock",
    "WireMock",
    "VertX",
    "Selenide",
    "JMockit",
    "Custom"
  })
  void test(String framework) {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AssertionsInTestsCheck/" + framework + ".java")
      .withCheck(check)
      .verifyIssues();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(
      "Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: 'blabla'",
      "Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: 'bla# '",
      "Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: ' #bla'");
  }

  @Test
  void testNoIssuesWithout() {
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AssertionsInTestsCheck/Junit3.java")
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testWithEmptyCustomAssertionMethods() {
    check.customAssertionMethods = "";
    JavaCheckVerifier.newVerifier()
      .onFile("src/test/files/checks/AssertionsInTestsCheck/Junit3.java")
      .withCheck(check)
      .verifyIssues();
    assertThat(logTester.logs(LoggerLevel.WARN))
      .doesNotContain("Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: ''");
  }
}
