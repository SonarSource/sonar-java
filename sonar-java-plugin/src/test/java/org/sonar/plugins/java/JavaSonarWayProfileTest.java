/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.java.checks.CheckList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class JavaSonarWayProfileTest {

  @Test
  public void should_create_sonar_way_profile() {
    ValidationMessages validation = ValidationMessages.create();

    JavaSonarWayProfile profileDef = new JavaSonarWayProfile();
    RulesProfile profile = profileDef.createProfile(validation);

    assertThat(profile.getLanguage()).isEqualTo(Java.KEY);
    List<ActiveRule> activeRules = profile.getActiveRulesByRepository(CheckList.REPOSITORY_KEY);
    assertThat(activeRules.size()).as("Expected number of rules in profile").isGreaterThanOrEqualTo(260);
    assertThat(profile.getName()).isEqualTo("Sonar way");
    Set<String> keys = new HashSet<>();
    for (ActiveRule activeRule : activeRules) {
      keys.add(activeRule.getRuleKey());
    }
    //Check that we store active rule with legacy keys, not RSPEC keys
    assertThat(keys.contains("S116")).isFalse();
    assertThat(keys).contains("S00116");
    assertThat(validation.hasErrors()).isFalse();
  }

}
