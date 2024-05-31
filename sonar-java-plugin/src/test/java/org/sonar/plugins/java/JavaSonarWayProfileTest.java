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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.ProfileRegistrar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.java.JavaSonarWayProfile.DBD_RULES_CLASS_NAME;

class JavaSonarWayProfileTest {

  @RegisterExtension
  public LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  SonarComponents sonarComponents = new SonarComponents(null,null,null,null,null,null);

  @Test
  void should_create_sonar_way_profile() {
    ValidationMessages validation = ValidationMessages.create();

    ProfileRegistrar fooCustomRules =
      registrarContext -> registrarContext.registerDefaultQualityProfileRules(List.of(
      RuleKey.of("javasecurity", "S6549"),
      RuleKey.of("javasecurity", "S6287")));
    ProfileRegistrar barCustomRules = registrarContext -> registrarContext.registerDefaultQualityProfileRules(List.of(
      RuleKey.of("javabugs", "S6466")));

    JavaSonarWayProfile profileDef = new JavaSonarWayProfile(new ProfileRegistrar[] {
      fooCustomRules,
      barCustomRules});
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profileDef.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("java", "Sonar way");
    assertThat(profile.language()).isEqualTo(Java.KEY);
    List<BuiltInQualityProfilesDefinition.BuiltInActiveRule> activeRules = profile.rules();
    assertThat(activeRules.stream().filter(r -> r.repoKey().equals("common-java"))).isEmpty();
    assertThat(activeRules).as("Expected number of rules in profile").hasSizeGreaterThanOrEqualTo(268);
    assertThat(profile.name()).isEqualTo("Sonar way");
    Set<RuleKey> keys = new HashSet<>();
    for (BuiltInQualityProfilesDefinition.BuiltInActiveRule activeRule : activeRules) {
      keys.add(RuleKey.of(activeRule.repoKey(), activeRule.ruleKey()));
    }
    // We no longer store active rules with legacy keys, only RSPEC keys are used.
    assertThat(keys)
      .doesNotContain(RuleKey.of("java", "S00116"))
      .contains(RuleKey.of("java", "S116"))
      .doesNotContain(RuleKey.of("java", "S6549"))
      .doesNotContain(RuleKey.of("javasecurity", "S116"))
      .contains(RuleKey.of("javasecurity", "S6549"))
      .contains(RuleKey.of("javasecurity", "S6287"))
      .contains(RuleKey.of("javabugs", "S6466"));
    assertThat(validation.hasErrors()).isFalse();

    // Check that we use severity from the read rule and not default one.
    assertThat(activeRules.get(0).overriddenSeverity()).isNull();
  }

  @Test
  void should_activate_hotspots_when_supported() {
    JavaSonarWayProfile profileDef = new JavaSonarWayProfile();
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profileDef.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("java", "Sonar way");
    BuiltInQualityProfilesDefinition.BuiltInActiveRule rule = profile.rule(RuleKey.of("java", "S2092"));
    assertThat(rule).isNotNull();
  }

  @Test
  void should_contains_security_rules_if_present() {
    // no security rules available
    com.sonar.plugins.security.api.JavaRules.ruleKeys = new HashSet<>();
    assertThat(JavaSonarWayProfile.getSecurityRuleKeys()).isEmpty();

    // one security rule available
    com.sonar.plugins.security.api.JavaRules.ruleKeys = new HashSet<>(Arrays.asList("S3649"));
    assertThat(JavaSonarWayProfile.getSecurityRuleKeys()).containsOnly(RuleKey.of("security-repo-key", "S3649"));
  }

  @Test
  void should_contains_dataflow_bug_detection_rules_if_present() {
    // no dataflow bug detection rules available
    com.sonarsource.plugins.dbd.api.JavaRules.ruleKeys = new HashSet<>();
    assertThat(JavaSonarWayProfile.getDataflowBugDetectionRuleKeys()).isEmpty();

    // one dataflow bug detection rule available
    com.sonarsource.plugins.dbd.api.JavaRules.ruleKeys = new HashSet<>(Arrays.asList("S6322"));
    assertThat(JavaSonarWayProfile.getDataflowBugDetectionRuleKeys()).containsOnly(RuleKey.of("dbd-repo-key", "S6322"));
  }

  @Test
  void external_rule_keys_missing_class() {
    JavaSonarWayProfile.getExternalRuleKeys("silly.name", "getDataflowBugDetectionRuleKeys", "ruleCategory");
    assertThat(logTester.logs(Level.DEBUG)).containsExactly("[ClassNotFoundException], no ruleCategory rules added to Sonar way java profile: silly.name");
  }

  @Test
  void external_rule_keys_missing_method() {
    JavaSonarWayProfile.getExternalRuleKeys(DBD_RULES_CLASS_NAME, "nonExistingRuleKeysMethod", "ruleCategory");
    assertThat(logTester.logs(Level.DEBUG))
      .containsExactly("[NoSuchMethodException], no ruleCategory rules added to Sonar way java profile: com.sonarsource.plugins.dbd.api.JavaRules.nonExistingRuleKeysMethod()");
  }

  @Test
  void external_rule_keys_method_throws_exception() {
    JavaSonarWayProfile.getExternalRuleKeys(DBD_RULES_CLASS_NAME, "methodThrowingException", "ruleCategory");
    assertThat(logTester.logs(Level.DEBUG)).containsExactly("[InvocationTargetException], no ruleCategory rules added to Sonar way java profile: null");
  }
}
