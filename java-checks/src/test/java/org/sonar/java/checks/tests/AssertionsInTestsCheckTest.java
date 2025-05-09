/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.tests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.testCodeSourcesPath;

class AssertionsInTestsCheckTest {

  private AssertionsInTestsCheck check = new AssertionsInTestsCheck();

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

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
    "Hamcrest",
    "Spring",
    "EasyMock",
    "Truth",
    "ReactiveX1",
    "ReactiveX2",
    "RestAssured",
    "RestAssured2",
    "Mockito",
    "JMock",
    "WireMock",
    "VertxJUnit4",
    "VertxJUnit5",
    "Selenide",
    "JMockit",
    "Awaitility",
    "AssertJ",
    "Custom"
  })
  void test(String framework) {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/" + framework + ".java"))
      .withCheck(check)
      .verifyIssues();
    assertThat(logTester.logs(Level.WARN)).contains(
      "Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: 'blabla'",
      "Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: 'bla# '",
      "Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: ' #bla'");
  }

  @Test
  void testNonCompilingCode() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/tests/AssertionsInTestsCheck/AssertJ.java"))
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void testNoIssuesWithoutSemantic() {
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/Junit3.java"))
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void testWithEmptyCustomAssertionMethods() {
    check.customAssertionMethods = "";
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/Junit3.java"))
      .withCheck(check)
      .verifyIssues();
    assertThat(logTester.logs(Level.WARN))
      .doesNotContain("Unable to create a corresponding matcher for custom assertion method, please check the format of the following symbol: ''");
  }

  @Test
  void testSpringBootSanity(){
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/SpringBootSanityTestSample.java"))
      .withCheck(check)
      .verifyIssues();

    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/SpringBootSanityJ4Test.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void testSpringBootAssertableApplicationContext(){
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/SpringBootAppContextRunnerSample.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void testSpringUtilAssert(){
    CheckVerifier.newVerifier()
      .onFile(testCodeSourcesPath("checks/tests/AssertionsInTestsCheck/SpringUtilAssertTestSample.java"))
      .withCheck(check)
      .verifyIssues();
  }

}
