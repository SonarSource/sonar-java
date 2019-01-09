/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
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

public class JavaRulesDefinitionTest {

  private Configuration settings;

  @Before
  public void setUp() {
    settings = new MapSettings().asConfig();
  }

  @Test
  public void test_creation_of_rules() {
    JavaRulesDefinition definition = new JavaRulesDefinition(settings, SonarVersion.SQ_73_RUNTIME);
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
    assertThat(repository.rule("S2095").activatedByDefault()).isEqualTo(true);
    RulesDefinition.Rule magicNumber = repository.rule("S109");
    assertThat(magicNumber.params()).isNotEmpty();
    assertThat(magicNumber.activatedByDefault()).isFalse();

    // check if a rule using a legacy key is also enabled
    RulesDefinition.Rule unusedPrivateMethodRule = repository.rule("UnusedPrivateMethod");
    assertThat(unusedPrivateMethodRule.activatedByDefault()).isEqualTo(true);

    // rule templates are manually defined
    assertThat(repository.rules().stream()
      .filter(RulesDefinition.Rule::template)
      .map(RulesDefinition.Rule::key)).containsOnly("S124", "S2253", "S3417", "S3688", "S3546", "S4011");

    // Calling definition multiple time should not lead to failure: thanks C# plugin !
    definition.define(new RulesDefinition.Context());
  }

  @Test
  public void rules_definition_should_be_locale_independent() {
    Locale defaultLocale = Locale.getDefault();
    Locale trlocale= Locale.forLanguageTag("tr-TR");
    Locale.setDefault(trlocale);
    JavaRulesDefinition definition = new JavaRulesDefinition(SonarVersion.SQ_73_RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    assertThat(repository.name()).isEqualTo("SonarAnalyzer");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size());
    Locale.setDefault(defaultLocale);
  }

  @Test
  public void debug_rules() {
    MapSettings settings = new MapSettings();
    settings.setProperty("sonar.java.debug", true);
    JavaRulesDefinition definition = new JavaRulesDefinition(settings.asConfig(), SonarVersion.SQ_73_RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    assertThat(repository.name()).isEqualTo("SonarAnalyzer");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size() + CheckList.getDebugChecks().size());
  }

  @Test
  public void test_invalid_checks() throws Exception {
    RulesDefinition.Context context = new RulesDefinition.Context();
    RulesDefinition.NewRepository newRepository = context.createRepository("test", "java");
    newRepository.createRule("correctRule");
    JavaRulesDefinition definition = new JavaRulesDefinition(settings, SonarVersion.SQ_73_RUNTIME);
    JavaSonarWayProfile.Profile profile = new JavaSonarWayProfile.Profile();
    profile.ruleKeys = new ArrayList<>();
    try {
      definition.newRule(CheckWithNoAnnotation.class, newRepository, profile);
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("No Rule annotation was found on class "+CheckWithNoAnnotation.class.getName());
    }

    try {
      definition.newRule(EmptyRuleKey.class, newRepository, profile);
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("No key is defined in Rule annotation of class "+EmptyRuleKey.class.getName());
    }

    try {
      definition.newRule(UnregisteredRule.class, newRepository, profile);
    } catch (IllegalStateException ise) {
      assertThat(ise).hasMessage("No rule was created for class "+UnregisteredRule.class.getName()+" in test");
    }
    // no metadata defined, does not fail on registration of rule
    definition.newRule(CorrectRule.class, newRepository, profile);

  }

  @Test
  public void test_security_hotspot() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition(settings, SonarVersion.SQ_73_RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    RulesDefinition.Rule hardcodedCredentialsRule = repository.rule("S1313");
    assertThat(hardcodedCredentialsRule.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    assertThat(hardcodedCredentialsRule.activatedByDefault()).isFalse();
  }

  @Test
  public void test_security_hotspot_lts() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition(settings, SonarVersion.SQ_67_RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    RulesDefinition.Rule hardcodedCredentialsRule = repository.rule("S1313");
    assertThat(hardcodedCredentialsRule.type()).isEqualTo(RuleType.VULNERABILITY);
    assertThat(hardcodedCredentialsRule.activatedByDefault()).isFalse();
  }

  @Test
  public void test_security_standards() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition(settings, SonarVersion.SQ_73_RUNTIME);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository("squid");

    RulesDefinition.Rule rule = repository.rule("S1989");
    assertThat(rule.securityStandards()).containsExactlyInAnyOrder("cwe:600", "owaspTop10:a3");
  }

  @Test
  public void test_security_standards_not_set_when_unsupported() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition(settings, SonarVersion.SQ_67_RUNTIME);
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
