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

class JavaSERulesDefinitionTest {

//  private static final String REPOSITORY_KEY = "java";
//  private static final SonarRuntime SONAR_RUNTIME_9_8 = SonarRuntimeImpl.forSonarQube(Version.create(9, 8), SonarQubeSide.SERVER, SonarEdition.COMMUNITY);
//
//  @Test
//  void test_creation_of_rules() {
//    JavaSERulesDefinition definition = new JavaSERulesDefinition(SONAR_RUNTIME_9_8);
//    RulesDefinition.Context context = new RulesDefinition.Context();
//    definition.define(context);
//    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
//
//    assertThat(repository.name()).isEqualTo("Sonar");
//    assertThat(repository.language()).isEqualTo("java");
//    assertThat(repository.rules()).hasSize(JavaSECheckList.getChecks().size());
//
//    RulesDefinition.Rule s2095 = repository.rule("S2095");
//    assertThat(s2095).isNotNull();
//    assertThat(s2095.type()).isEqualTo(RuleType.BUG);
//    assertThat(s2095.internalKey()).isNull();
//    assertThat(s2095.name()).isEqualTo("Resources should be closed");
//
//    RulesDefinition.Rule s2189 = repository.rule("S2589");
//    assertThat(s2189.type()).isEqualTo(RuleType.CODE_SMELL);
//    assertThat(s2189.activatedByDefault()).isTrue();
//
//    assertThat(repository.rule("S6374").activatedByDefault()).isFalse();
//
//    // rule templates are manually defined
//    assertThat(repository.rules().stream()
//      .filter(RulesDefinition.Rule::template)
//      .map(RulesDefinition.Rule::key)).containsOnly("S3546");
//
//    // Calling definition multiple time should not lead to failure: thanks C# plugin !
//    definition.define(new RulesDefinition.Context());
//  }
//
//  @Test
//  void rules_definition_should_be_locale_independent() {
//    Locale defaultLocale = Locale.getDefault();
//    Locale trlocale = Locale.forLanguageTag("tr-TR");
//    Locale.setDefault(trlocale);
//    JavaSERulesDefinition definition = new JavaSERulesDefinition(SONAR_RUNTIME_9_8);
//    RulesDefinition.Context context = new RulesDefinition.Context();
//    definition.define(context);
//    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
//
//    assertThat(repository.name()).isEqualTo("Sonar");
//    assertThat(repository.language()).isEqualTo("java");
//    assertThat(repository.rules()).hasSize(JavaSECheckList.getChecks().size());
//    Locale.setDefault(defaultLocale);
//  }
//
//  @Test
//  void test_deprecated_key() {
//    JavaSERulesDefinition definition = new JavaSERulesDefinition(SONAR_RUNTIME_9_8);
//    RulesDefinition.Context context = new RulesDefinition.Context();
//    definition.define(context);
//    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
//
//    // FIXME SONAR-17167: Rules can not have multiple links to deprecated keys, especially if one of the deprecated key is a droppped rule
//    List<String> rulesWithManyDeprecatedKeys = repository.rules().stream()
//      .filter(r -> r.deprecatedRuleKeys().size() >= 2)
//      .map(RulesDefinition.Rule::key)
//      .toList();
//    assertThat(rulesWithManyDeprecatedKeys).isEmpty();
//  }
//
//  @Test
//  void rules_should_not_have_legacy_key() {
//    JavaSERulesDefinition definition = new JavaSERulesDefinition(SONAR_RUNTIME_9_8);
//    RulesDefinition.Context context = new RulesDefinition.Context();
//    definition.define(context);
//    Pattern pattern = Pattern.compile("^S[0-9]{3,5}$");
//    RulesDefinition.Repository repository = context.repository(REPOSITORY_KEY);
//    repository.rules().forEach(r -> {
//      // NoSonar key can't be changed to RSPEC key
//      if (!r.key().equals("NoSonar")) {
//        assertThat(pattern.matcher(r.key()).matches()).isTrue();
//        assertThat(r.internalKey()).isNull();
//      } else {
//        assertThat(r.internalKey()).isEqualTo("S1291");
//      }
//    });
//  }

}
