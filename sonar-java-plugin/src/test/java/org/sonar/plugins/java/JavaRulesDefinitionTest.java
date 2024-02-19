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
package org.sonar.plugins.java;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKeys;

import static org.assertj.core.api.Assertions.assertThat;

class JavaRulesDefinitionTest {

  private static final String REPOSITORY_KEY = "java";
  private static final SonarRuntime SONAR_RUNTIME_9_2 = SonarRuntimeImpl.forSonarLint(Version.create(9, 2));
  private static final SonarRuntime SONAR_RUNTIME_9_8 = SonarRuntimeImpl.forSonarQube(Version.create(9, 8), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);

  @Test
  void test_creation_of_rules() {
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_8);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo("Sonar");
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
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_8);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    assertThat(repository.name()).isEqualTo("Sonar");
    assertThat(repository.language()).isEqualTo("java");
    assertThat(repository.rules()).hasSize(CheckList.getChecks().size());
    Locale.setDefault(defaultLocale);
  }

  @Test
  void test_security_hotspot() throws Exception {
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_2);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule hardcodedIdRule = repository.rule("S1313");
    assertThat(hardcodedIdRule.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "S1313"));
    assertThat(hardcodedIdRule.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
    // SonarLint explicitly exclude hotspot on its side.
    assertThat(hardcodedIdRule.activatedByDefault()).isTrue();
  }

  @Test
  void test_security_standards() {
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_8);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule s1166 = repository.rule("S1166");
    assertThat(s1166.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "S1166"));
    assertThat(s1166.securityStandards()).containsExactlyInAnyOrder("cwe:778", "owaspTop10:a10", "owaspTop10-2021:a9");

    RulesDefinition.Rule s2053 = repository.rule("S2053");
    assertThat(s2053.securityStandards()).containsExactlyInAnyOrder("cwe:759", "cwe:760", "owaspTop10:a3", "owaspTop10-2021:a2", "pciDss-3.2:6.5.10", "pciDss-4.0:6.2.4");
  }

  @Test
  void test_security_standards_sq_9_2() {
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_2);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule s1166 = repository.rule("S1166");
    assertThat(s1166.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "S1166"));
    assertThat(s1166.securityStandards()).containsExactlyInAnyOrder("cwe:778", "owaspTop10:a10");
  }

  @Test
  void test_deprecated_key() {
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_8);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);

    RulesDefinition.Rule rule = repository.rule("S1104");
    assertThat(rule.activatedByDefault()).isTrue();
    assertThat(rule.deprecatedRuleKeys()).containsExactly(RuleKey.of("squid", "ClassVariableVisibilityCheck"));

    // FIXME SONAR-17167: S4830 should have references to java:S4244 and squid:S4244
    RulesDefinition.Rule s4830 = repository.rule("S4830");
    assertThat(s4830.deprecatedRuleKeys()).containsExactlyInAnyOrder(RuleKey.of("squid", "S4830"));

    // FIXME SONAR-17167: Rules can not have multiple links to deprecated keys, especially if one of the deprecated key is a droppped rule
    List<String> rulesWithManyDeprecatedKeys = repository.rules().stream()
      .filter(r -> r.deprecatedRuleKeys().size() >= 2)
      .map(RulesDefinition.Rule::key)
      .toList();
    assertThat(rulesWithManyDeprecatedKeys).isEmpty();
  }

  @Test
  void test_deprecates_rules() {
    @DeprecatedRuleKey(repositoryKey = "repo", ruleKey = "SXXXX")
    class RuleA {
    }

    @DeprecatedRuleKey(repositoryKey = "repo", ruleKey = "SXXXX")
    @DeprecatedRuleKey(repositoryKey = "repo", ruleKey = "SYYYY")
    class RuleB {
    }

    @DeprecatedRuleKeys({
      @DeprecatedRuleKey(repositoryKey = "repo", ruleKey = "SXXXX"),
      @DeprecatedRuleKey(repositoryKey = "repo", ruleKey = "SYYYY")
    })
    class RuleC {
    }

    class RuleD {
    }

    assertThat(JavaRulesDefinition.deprecatesRules(RuleA.class)).isTrue();
    assertThat(JavaRulesDefinition.deprecatesRules(RuleB.class)).isTrue();
    assertThat(JavaRulesDefinition.deprecatesRules(RuleC.class)).isTrue();
    assertThat(JavaRulesDefinition.deprecatesRules(RuleD.class)).isFalse();
  }

  @Test
  void rules_should_not_have_legacy_key() {
    JavaRulesDefinition definition = new JavaRulesDefinition(SONAR_RUNTIME_9_8);
    RulesDefinition.Context context = new RulesDefinition.Context();
    definition.define(context);
    Pattern pattern = Pattern.compile("^S[0-9]{3,5}$");
    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
    repository.rules().forEach(r -> {
      // NoSonar key can't be changed to RSPEC key
      if (!r.key().equals("NoSonar")) {
        assertThat(pattern.matcher(r.key()).matches()).isTrue();
        assertThat(r.internalKey()).isNull();
      } else {
        assertThat(r.internalKey()).isEqualTo("S1291");
      }
    });
  }

}
