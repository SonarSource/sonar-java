/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.SonarRuntime;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.ValidationMessages;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaSonarWayProfileTest {

  @Test
  public void should_create_sonar_way_profile() {
    ValidationMessages validation = ValidationMessages.create();

    JavaSonarWayProfile profileDef = new JavaSonarWayProfile(SonarVersion.SQ_73_RUNTIME);
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profileDef.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("java", "Sonar way");
    assertThat(profile.language()).isEqualTo(Java.KEY);
    List<BuiltInQualityProfilesDefinition.BuiltInActiveRule> activeRules = profile.rules();
    assertThat(activeRules.stream().filter(r -> r.repoKey().equals("common-java"))).hasSize(1);
    assertThat(activeRules.size()).as("Expected number of rules in profile").isGreaterThanOrEqualTo(268);
    assertThat(profile.name()).isEqualTo("Sonar way");
    Set<String> keys = new HashSet<>();
    for (BuiltInQualityProfilesDefinition.BuiltInActiveRule activeRule : activeRules) {
      keys.add(activeRule.ruleKey());
    }
    //Check that we store active rule with legacy keys, not RSPEC keys
    assertThat(keys.contains("S116")).isFalse();
    assertThat(keys).contains("S00116");
    assertThat(validation.hasErrors()).isFalse();

    // Check that we use severity from the read rule and not default one.
    assertThat(activeRules.get(0).overriddenSeverity()).isNull();
  }

  @Test
  public void should_activate_hotspots_when_supported() {
    JavaSonarWayProfile profileDef = new JavaSonarWayProfile(SonarVersion.SQ_73_RUNTIME);
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profileDef.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("java", "Sonar way");
    BuiltInQualityProfilesDefinition.BuiltInActiveRule rule = profile.rule(RuleKey.of("squid", "S2092"));
    assertThat(rule).isNotNull();
  }

  @Test
  public void should_not_activate_hotspots_when_not_supported() {
    JavaSonarWayProfile profileDef = new JavaSonarWayProfile(SonarVersion.SQ_67_RUNTIME);
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profileDef.define(context);
    BuiltInQualityProfilesDefinition.BuiltInQualityProfile profile = context.profile("java", "Sonar way");
    BuiltInQualityProfilesDefinition.BuiltInActiveRule rule = profile.rule(RuleKey.of("squid", "S2092"));
    assertThat(rule).isNull();
  }

}
