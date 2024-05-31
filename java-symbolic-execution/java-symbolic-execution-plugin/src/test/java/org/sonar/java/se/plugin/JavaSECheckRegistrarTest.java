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
package org.sonar.java.se.plugin;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.check.Rule;
import org.sonar.java.checks.verifier.TestCheckRegistrarContext;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.CheckRegistrar;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSECheckRegistrarTest {

  private static final ActiveRules activeRules = activeRules(getRuleKeysWithRepo());

  @Test
  void register_rules() {
    CheckRegistrar registrar = new JavaSECheckRegistrar(null);
    TestCheckRegistrarContext context = new TestCheckRegistrarContext();

    CheckFactory checkFactory = new CheckFactory(activeRules);
    registrar.register(context, checkFactory);

    assertThat(context.mainRuleKeys).map(RuleKey::toString).containsExactlyInAnyOrder(getRuleKeysWithRepo());
    assertThat(context.testRuleKeys).isEmpty();
  }

  @Test
  void rules_definition() {
    SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarQube(Version.create(10, 2), SonarQubeSide.SERVER, SonarEdition.ENTERPRISE);
    JavaSECheckRegistrar rulesDefinition = new JavaSECheckRegistrar(sonarRuntime);
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository javaRepo = context
      .createRepository("java", "java")
      .setName("SonarAnalyzer");
    rulesDefinition.customRulesDefinition(context, javaRepo);
    javaRepo.done();

    RulesDefinition.Repository oldRepository = context.repository("squid");
    assertThat(oldRepository).isNull();

    RulesDefinition.Repository repository = context.repository(JavaSECheckRegistrar.REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo("Sonar");
    assertThat(repository.language()).isEqualTo("java");
    List<RulesDefinition.Rule> rules = repository.rules();
    assertThat(rules).hasSize(23);

    var activeByDefault = rules.stream()
      .filter(k -> !"S6374".equals(k.key()) && !"S3546".equals(k.key()))
      .toList();
    var allRules = rules.stream().map(RulesDefinition.Rule::key).toList();

    assertThat(Arrays.asList(getRuleKeys())).containsExactlyInAnyOrderElementsOf(allRules);
    assertThat(activeByDefault).isNotEmpty().allMatch(RulesDefinition.Rule::activatedByDefault);
  }

  private static ActiveRules activeRules(String... repositoryAndKeys) {
    ActiveRulesBuilder activeRules = new ActiveRulesBuilder();
    for (String repositoryAndKey : repositoryAndKeys) {
      activeRules.addRule(new NewActiveRule.Builder()
        .setRuleKey(RuleKey.parse(repositoryAndKey))
        .setLanguage("java")
        .build());
    }
    return activeRules.build();
  }

  private static String[] getRuleKeysWithRepo() {
    var ruleKeys = getRuleKeys();
    for (int i = 0; i < ruleKeys.length; i++) {
      ruleKeys[i] = "java:" + ruleKeys[i];
    }
    return ruleKeys;
  }

  private static String[] getRuleKeys() {
    var ruleKeys = new ArrayList<String>();
    for (Class<? extends SECheck> check : JavaSECheckList.getChecks()) {
      ruleKeys.add(check.getAnnotation(Rule.class).key());
    }
    return ruleKeys.toArray(new String[0]);
  }

}
