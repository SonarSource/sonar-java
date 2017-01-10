/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.plugins.java;

import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Cardinality;
import org.sonar.check.Rule;
import org.sonar.java.checks.CheckList;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaRulesDefinitionTest {

  @Test
  public void test() {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    assertThat(repository.name()).isEqualTo("SonarAnalyzer");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size());

    RulesDefinition.Rule unusedLabelRule = repository.rule("S1065");
    assertThat(unusedLabelRule).isNotNull();
    assertThat(unusedLabelRule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(unusedLabelRule.internalKey()).isNull();
    assertThat(unusedLabelRule.name()).isEqualTo("Unused labels should be removed");
    assertThat(repository.rule("S2095").type()).isEqualTo(RuleType.BUG);
    RulesDefinition.Rule magicNumber = repository.rule("S109");
    assertThat(magicNumber.params()).isNotEmpty();
    // Calling definition multiple time should not lead to failure: thanks C# plugin !
    definition.define(new RulesDefinition.Context());
  }

  @Test
  public void test_invalid_checks() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository newRepository = context.createRepository("test", "java");
    newRepository.createRule("myCardinality");
    newRepository.createRule("correctRule");
    JavaRulesDefinition definition = new JavaRulesDefinition();
    try {
      definition.newRule(CheckWithNoAnnotation.class, newRepository);
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("No Rule annotation was found on class "+CheckWithNoAnnotation.class.getName());
    }

    try {
      definition.newRule(EmptyRuleKey.class, newRepository);
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("No key is defined in Rule annotation of class "+EmptyRuleKey.class.getName());
    }

    try {
      definition.newRule(UnregisteredRule.class, newRepository);
    } catch (IllegalStateException ise) {
      assertThat(ise).hasMessage("No rule was created for class "+UnregisteredRule.class.getName()+" in test");
    }
    try {
      definition.newRule(CardinalityRule.class, newRepository);
    } catch (IllegalArgumentException ise) {
      assertThat(ise).hasMessage("Cardinality is not supported, use the RuleTemplate annotation instead for class "+CardinalityRule.class.getName());
    }
    // no metadata defined, does not fail on registration of rule
    definition.newRule(CorrectRule.class, newRepository);

  }

  private class CheckWithNoAnnotation implements JavaCheck {
  }

  @Rule(key = "")
  private class EmptyRuleKey implements JavaCheck {
  }

  @Rule(key = "myKey")
  private class UnregisteredRule implements JavaCheck {
  }

  @Rule(key = "myCardinality", cardinality = Cardinality.MULTIPLE)
  private class CardinalityRule implements JavaCheck {
  }

  @Rule(key = "correctRule")
  private class CorrectRule implements JavaCheck {
  }

}
