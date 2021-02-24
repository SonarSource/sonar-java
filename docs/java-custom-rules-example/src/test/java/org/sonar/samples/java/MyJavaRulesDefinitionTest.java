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
package org.sonar.samples.java;

import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.DebtRemediationFunction.Type;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.NewRepository;
import org.sonar.api.server.rule.RulesDefinition.Param;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.server.rule.RulesDefinition.Rule;
import org.sonar.api.server.rule.RulesDefinitionAnnotationLoader;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.samples.java.annotations.RuleTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MyJavaRulesDefinitionTest {

  @Test
  void test() {
    MyJavaRulesDefinition rulesDefinition = new MyJavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    RulesDefinition.Repository repository = context.repository(MyJavaRulesDefinition.REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo("MyCompany Custom Repository");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(RulesList.getChecks().size());

    assertRuleProperties(repository);
    assertParameterProperties(repository);
    assertAllRuleParametersHaveDescription(repository);
  }

  /**
   * This nested class only verifies the good behavior of the metadata and description loader
   */
  @Nested
  class RuleLoaderTests {
    private NewRepository repository;
    private RulesDefinition.Context context;

    @BeforeEach
    void setup() {
      context = new RulesDefinition.Context();
      repository = context.createRepository("repo", "java").setName("test");
    }

    @Test
    void rule_without_annotation_should_fail() {
      assertThatThrownBy(() -> MyJavaRulesDefinition.newRule(RuleWithoutAnnotation.class, repository))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No Rule annotation was found on class org.sonar.samples.java.MyJavaRulesDefinitionTest")
        .hasMessageEndingWith("RuleWithoutAnnotation");
    }

    @Test
    void rule_without_key_should_fail() {
      assertThatThrownBy(() -> MyJavaRulesDefinition.newRule(RuleWithoutKey.class, repository))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No key is defined in Rule annotation of class org.sonar.samples.java.MyJavaRulesDefinitionTest")
        .hasMessageEndingWith("RuleWithoutKey");
    }

    @Test
    void rule_key_should_be_unique() {
      assertThatThrownBy(() -> MyJavaRulesDefinition.newRule(RuleWithKey.class, repository))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No rule was created for class org.sonar.samples.java.MyJavaRulesDefinitionTest")
        .hasMessageEndingWith("RuleWithKey in repo");
    }

    @Test
    void rules_can_be_created_with_nothing_else_than_a_key_to_define_them() {
      new RulesDefinitionAnnotationLoader().load(repository, RuleWithKey.class);
      MyJavaRulesDefinition.newRule(RuleWithKey.class, repository);

      assertThat(repository.rules()).hasSize(1);
    }

    @Test
    void rules_with_wrong_metadata_should_fail() {
      new RulesDefinitionAnnotationLoader().load(repository, RuleWithWrongMetadata.class);
      assertThatThrownBy(() -> MyJavaRulesDefinition.newRule(RuleWithWrongMetadata.class, repository))
        .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rules_should_allow_linear_remediation_fonction() {
      Rule rule = initRule(RuleWithLinearRemediationFunction.class, "r3");
      assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    }

    @Test
    void rules_should_allow_linear_with_offset_remediation_fonction() {
      Rule rule = initRule(RuleWithLinearWithOffsetRemediationFunction.class, "r4");
      assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    }

    @Test
    void rules_should_allow_no_remediation_fonction() {
      Rule rule = initRule(RuleWithoutRemediationFunction.class, "r5");
      assertThat(rule.debtRemediationFunction()).isNull();
    }

    @Test
    void rules_templates_should_be_recognized() {
      Rule rule = initRule(TemplateRuleWithKey.class, "t1");
      assertThat(rule.template()).isTrue();
    }

    @Test
    void missing_resource_should_fail() throws Exception {
      URL fakeFike = new File("doesNotExist.txt").toURI().toURL();
      assertThatThrownBy(() -> MyJavaRulesDefinition.readResource(fakeFike))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Failed to read: ")
        .hasMessageEndingWith("doesNotExist.txt");
    }

    private Rule initRule(Class<? extends JavaCheck> ruleClass, String key) {
      new RulesDefinitionAnnotationLoader().load(repository, ruleClass);
      MyJavaRulesDefinition.newRule(ruleClass, repository);

      assertThat(repository.rules()).hasSize(1);
      repository.done();

      return context.repository("repo").rule(key);
    }

    // test classes
    class RuleWithoutAnnotation implements JavaCheck { }
    @org.sonar.check.Rule class RuleWithoutKey implements JavaCheck { }
    @org.sonar.check.Rule(key = "r1") class RuleWithKey implements JavaCheck { }
    @org.sonar.check.Rule(key = "r2") class RuleWithWrongMetadata implements JavaCheck { }
    @org.sonar.check.Rule(key = "r3") class RuleWithLinearRemediationFunction implements JavaCheck { }
    @org.sonar.check.Rule(key = "r4") class RuleWithLinearWithOffsetRemediationFunction implements JavaCheck { }
    @org.sonar.check.Rule(key = "r5") class RuleWithoutRemediationFunction implements JavaCheck { }
    @org.sonar.check.Rule(key = "t1", name = "template") @RuleTemplate class TemplateRuleWithKey implements JavaCheck { }
  }

  private static void assertParameterProperties(Repository repository) {
    Param max = repository.rule("AvoidAnnotation").param("name");
    assertThat(max).isNotNull();
    assertThat(max.defaultValue()).isEqualTo("Inject");
    assertThat(max.description()).isEqualTo("Name of the annotation to avoid, without the prefix @, for instance 'Override'");
    assertThat(max.type()).isEqualTo(RuleParamType.STRING);
  }

  private static void assertRuleProperties(Repository repository) {
    Rule rule = repository.rule("AvoidAnnotation");
    assertThat(rule).isNotNull();
    assertThat(rule.name()).isEqualTo("Title of AvoidAnnotation");
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(Type.CONSTANT_ISSUE);
    assertThat(rule.type()).isEqualTo(RuleType.CODE_SMELL);
  }

  private static void assertAllRuleParametersHaveDescription(Repository repository) {
    for (Rule rule : repository.rules()) {
      for (Param param : rule.params()) {
        assertThat(param.description()).as("description for " + param.key()).isNotEmpty();
      }
    }
  }

}
