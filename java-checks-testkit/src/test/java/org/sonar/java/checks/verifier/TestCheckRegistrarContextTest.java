/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.verifier;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestCheckRegistrarContextTest {

  @Rule(key = "X1")
  static class Rule1 implements JavaCheck {
  }
  @Rule(key = "X2")
  static class Rule2 implements JavaCheck {
  }
  @Rule(key = "X3")
  static class Rule3 implements JavaCheck {
  }
  @Rule(key = "X4")
  static class Rule4 implements JavaCheck {
  }
  @Rule(key = "X5")
  static class Rule5 implements JavaCheck {
  }
  @Rule(key = "X6")
  static class Rule6 implements JavaCheck {
  }
  static class Scanner implements JavaCheck {
  }

  @Test
  void store_registration() {
    TestCheckRegistrarContext context = new TestCheckRegistrarContext();

    var rule4 = new Rule4();

    context.registerClassesForRepository(
      "customRepo",
      List.of(Rule1.class, Rule2.class),
      List.of(Rule1.class, Rule3.class));

    context.registerMainChecks("customRepo", List.of(
      rule4,
      Rule5.class));
    context.registerTestChecks("customRepo", List.of(
      rule4,
      Rule6.class));
    context.registerMainSharedCheck(new Scanner(), List.of(
      RuleKey.of("securityRepo", "R1"),
      RuleKey.of("securityRepo", "R2")));
    context.registerTestSharedCheck(new Scanner(), List.of(
      RuleKey.of("securityRepo", "R3"),
      RuleKey.of("securityRepo", "R4")));
    context.registerAutoScanCompatibleRules(List.of(
      RuleKey.of("customRepo", "X1")));

    assertThat(context.mainRuleKeys).extracting(RuleKey::toString).containsExactly(
      "customRepo:X1",
      "customRepo:X2",
      "customRepo:X4",
      "customRepo:X5",
      "securityRepo:R1",
      "securityRepo:R2");

    assertThat(context.mainCheckClasses).extracting(Class::getSimpleName).containsExactly(
      "Rule1",
      "Rule2",
      "Rule4",
      "Rule5",
      "Scanner");

    assertThat(context.testRuleKeys).extracting(RuleKey::toString).containsExactly(
      "customRepo:X1",
      "customRepo:X3",
      "customRepo:X4",
      "customRepo:X6",
      "securityRepo:R3",
      "securityRepo:R4");

    assertThat(context.testCheckClasses).extracting(Class::getSimpleName).containsExactly(
      "Rule1",
      "Rule3",
      "Rule4",
      "Rule6",
      "Scanner");

    assertThat(context.autoScanCompatibleRules).containsExactly(RuleKey.of("customRepo", "X1"));
  }

  @Test
  void should_fail_if_repository_key_is_blank() {
    TestCheckRegistrarContext context = new TestCheckRegistrarContext();
    List<?> checkClasses = List.of(Rule1.class);
    assertThatThrownBy(() -> context.registerMainChecks(" ", checkClasses))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Please specify a non blank repository key");
  }

  @Test
  void should_fail_if_not_a_JavaCheck() {
    TestCheckRegistrarContext context = new TestCheckRegistrarContext();
    List<?> checkClasses = List.of(Object.class);
    assertThatThrownBy(() -> context.registerMainChecks("repo", checkClasses))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to instantiate class java.lang.Object");
  }

}
