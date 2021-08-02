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
package org.sonar.plugins.java;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Rule;
import org.sonar.java.checks.CheckList;
import org.sonar.java.checks.ServletMethodsExceptionsThrownCheck;
import org.sonar.plugins.java.api.JavaCheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaRulesDefinitionTest {

  private static final String REPOSITORY_KEY = "java";

  @Test
  void test_creation_of_rules() {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo("SonarQube");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size());

    RulesDefinition.Rule unusedLabelRule = repository.rule("S1065");
    assertThat(unusedLabelRule).isNotNull();
    assertThat(unusedLabelRule.type()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(unusedLabelRule.internalKey()).isNull();
    assertThat(unusedLabelRule.name()).isEqualTo("Unused labels should be removed");
    assertThat(repository.rule("S2095").type()).isEqualTo(RuleType.BUG);
    assertThat(repository.rule("S2095").deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "S2095"));
    assertThat(repository.rule("S2095").activatedByDefault()).isTrue();
    RulesDefinition.Rule magicNumber = repository.rule("S109");
    assertThat(magicNumber.params()).isNotEmpty();
    assertThat(magicNumber.activatedByDefault()).isFalse();

    // rule templates are manually defined
    assertThat(repository.rules().stream()
      .filter(RulesDefinition.Rule::template)
      .map(RulesDefinition.Rule::key)).containsOnly("S124", "S2253", "S3688", "S3546", "S4011");

    // Calling definition multiple time should not lead to failure: thanks C# plugin !
    definition.define(new RulesDefinition.Context());
  }

  @Test
  void rules_definition_should_be_locale_independent() {
    Locale defaultLocale = Locale.getDefault();
    Locale trlocale = Locale.forLanguageTag("tr-TR");
    Locale.setDefault(trlocale);
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo("SonarQube");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size());
    Locale.setDefault(defaultLocale);
  }

  @Test
  void test_invalid_checks() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository newRepository = context.createRepository("test", "java");
    newRepository.createRule("correctRule");
    JavaRulesDefinition definition = new JavaRulesDefinition();
    JavaSonarWayProfile.Profile profile = new JavaSonarWayProfile.Profile();
    profile.ruleKeys = new LinkedHashSet<>();
    try {
      definition.newRule(CheckWithNoAnnotation.class, newRepository, profile);
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("No Rule annotation was found on class " + CheckWithNoAnnotation.class.getName());
    }

    try {
      definition.newRule(EmptyRuleKey.class, newRepository, profile);
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("No key is defined in Rule annotation of class " + EmptyRuleKey.class.getName());
    }

    try {
      definition.newRule(UnregisteredRule.class, newRepository, profile);
    } catch (IllegalStateException ise) {
      assertThat(ise).hasMessage("No rule was created for class " + UnregisteredRule.class.getName() + " in test");
    }
    // no metadata defined, does not fail on registration of rule
    definition.newRule(CorrectRule.class, newRepository, profile);

  }

  @Test
  void test_security_hotspot() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule hardcodedCredentialsRule = repository.rule("S1313");
    assertThat(hardcodedCredentialsRule.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "S1313"));
    assertThat(hardcodedCredentialsRule.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(hardcodedCredentialsRule.activatedByDefault()).isFalse();
  }

  @Test
  void test_security_standards() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule rule = repository.rule("S1989");
    assertThat(rule.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "S1989"));
    assertThat(rule.securityStandards()).containsExactlyInAnyOrder("cwe:600", "owaspTop10:a3");
  }

  @Test
  void test_deprecated_key() {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule rule = repository.rule("S1104");
    assertThat(rule.activatedByDefault()).isTrue();
    assertThat(rule.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "ClassVariableVisibilityCheck"));
  }

  @Test
  void rules_should_not_have_legacy_key() {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    Pattern pattern = Pattern.compile("^S[0-9]{3,5}$");
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
    repository.rules().forEach(r -> {
      // NoSonar key can't be changed to RSPEC key
      if (!r.key().equals("NoSonar")) {
        assertThat(pattern.matcher(r.key()).matches()).isTrue();
      }
    });
  }

  @Test
  void test_security_standards_not_set_when_unsupported() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition();
    RulesDefinition.NewRepository repository = mock(RulesDefinition.NewRepository.class);
    RulesDefinition.NewRule newRule = mock(RulesDefinition.NewRule.class);
    when(repository.rule(any())).thenReturn(newRule);
    definition.newRule(ServletMethodsExceptionsThrownCheck.class, repository, JavaSonarWayProfile.readProfile());

    verify(newRule, never()).addOwaspTop10();
    verify(newRule, never()).addCwe();
  }

  private class CheckWithNoAnnotation implements JavaCheck {
  }

  @Rule(key = "")
  private class EmptyRuleKey implements JavaCheck {
  }

  @Rule(key = "myKey")
  private class UnregisteredRule implements JavaCheck {
  }

  @Rule(key = "correctRule")
  private class CorrectRule implements JavaCheck {
  }

}
