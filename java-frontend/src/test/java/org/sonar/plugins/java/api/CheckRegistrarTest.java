/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.rule.RuleKey;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckRegistrarTest {

  TestInternalRegistration registrarContext = new TestInternalRegistration();

  @Test
  void repository_key_is_mandatory() {
    List<Class<? extends JavaCheck>> emptyList = emptyList();
    assertThatThrownBy(() -> registrarContext.registerClassesForRepository("  ", emptyList, emptyList))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Please specify a valid repository key to register your custom rules");
  }

  @Test
  void main_and_test_classes_registration() {
    class MainCheckA implements JavaCheck {
    }
    class MainCheckB implements JavaCheck {
    }
    class MainCheckC implements JavaCheck {
    }
    class TestCheckD implements JavaCheck {
    }
    class TestCheckE implements JavaCheck {
    }
    class Scanner implements JavaCheck {
    }

    registrarContext.registerClassesForRepository("my-repo", null, null);
    registrarContext.registerClassesForRepository("my-repo",
      List.of(MainCheckA.class, MainCheckB.class),
      List.of(TestCheckD.class));
    registrarContext.registerMainChecks("my-repo",
      List.of(MainCheckC.class));
    registrarContext.registerTestChecks("my-repo",
      List.of(TestCheckE.class));
    registrarContext.registerMainSharedCheck(new Scanner(),
      List.of(RuleKey.of("repo", "S123")));
    registrarContext.registerTestSharedCheck(new Scanner(),
      List.of(RuleKey.of("repo", "S456")));
    registrarContext.registerAutoScanCompatibleRules(List.of(
      RuleKey.of("repo", "S123"),
      RuleKey.of("repo", "S456")));

    Checks<JavaCheck> checks = mock(Checks.class);
    when(checks.all()).thenReturn(List.of(new MainCheckA(), new MainCheckB(), new MainCheckC()));
    registrarContext.registerMainChecks(checks, List.of());
    registrarContext.registerTestChecks(checks, List.of());

    assertThat(registrarContext.repositoryKey()).isEqualTo("my-repo");
    assertThat(registrarContext.checkClasses()).hasSize(2);
    assertThat(registrarContext.testCheckClasses()).hasSize(1);

    assertThat(registrarContext.events).containsExactly(
      "register {} main checks in repository my-repo",
      "register {} test checks in repository my-repo",
      "register {MainCheckA, MainCheckB} main checks in repository my-repo",
      "register {TestCheckD} test checks in repository my-repo",
      "register {MainCheckC} main checks in repository my-repo",
      "register {TestCheckE} test checks in repository my-repo",
      "register Scanner for 1 main rules.",
      "register Scanner for 1 test rules.",
      "register 2 autoscan rules.",
      "register 3 instantiated main checks.",
      "register 3 instantiated test checks.");
  }

  private static class TestInternalRegistration extends CheckRegistrar.RegistrarContext {

    public final List<String> events = new ArrayList<>();

    @Override
    public void registerMainChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
      super.registerMainChecks(repositoryKey, javaCheckClassesAndInstances);
      String names = javaCheckClassesAndInstances.stream().map(o -> ((Class) o).getSimpleName()).collect(Collectors.joining(", "));
      events.add("register {" + names + "} main checks in repository " + repositoryKey);
    }

    @Override
    public void registerTestChecks(String repositoryKey, Collection<?> javaCheckClassesAndInstances) {
      super.registerTestChecks(repositoryKey, javaCheckClassesAndInstances);
      String names = javaCheckClassesAndInstances.stream().map(o -> ((Class) o).getSimpleName()).collect(Collectors.joining(", "));
      events.add("register {" + names + "} test checks in repository " + repositoryKey);
    }

    @Override
    public void registerMainSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
      super.registerMainSharedCheck(check, ruleKeys);
      events.add("register " + check.getClass().getSimpleName() + " for " + ruleKeys.size() + " main rules.");
    }

    @Override
    public void registerTestSharedCheck(JavaCheck check, Collection<RuleKey> ruleKeys) {
      super.registerTestSharedCheck(check, ruleKeys);
      events.add("register " + check.getClass().getSimpleName() + " for " + ruleKeys.size() + " test rules.");
    }

    @Override
    public void registerAutoScanCompatibleRules(Collection<RuleKey> ruleKeys) {
      super.registerAutoScanCompatibleRules(ruleKeys);
      events.add("register " + ruleKeys.size() + " autoscan rules.");
    }

    @Override
    public void registerMainChecks(Checks<JavaCheck> checks, Collection<?> javaCheckClassesAndInstances) {
      super.registerMainChecks(checks, javaCheckClassesAndInstances);
      events.add("register " + checks.all().size() + " instantiated main checks.");
    }

    @Override
    public void registerTestChecks(Checks<JavaCheck> checks, Collection<?> javaCheckClassesAndInstances) {
      super.registerTestChecks(checks, javaCheckClassesAndInstances);
      events.add("register " + checks.all().size() + " instantiated test checks.");
    }
  }
}
