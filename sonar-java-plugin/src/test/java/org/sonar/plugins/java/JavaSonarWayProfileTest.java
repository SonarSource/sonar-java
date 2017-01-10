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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.java.checks.CheckList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaSonarWayProfileTest {

  @Test
  public void should_create_sonar_way_profile() {
    ValidationMessages validation = ValidationMessages.create();

    JavaSonarWayProfile profileDef = new JavaSonarWayProfile(ruleFinder(false));
    RulesProfile profile = profileDef.createProfile(validation);

    assertThat(profile.getLanguage()).isEqualTo(Java.KEY);
    List<ActiveRule> activeRules = profile.getActiveRulesByRepository(CheckList.REPOSITORY_KEY);
    assertThat(profile.getActiveRulesByRepository("common-java")).hasSize(1);
    assertThat(activeRules.size()).as("Expected number of rules in profile").isGreaterThanOrEqualTo(270);
    assertThat(profile.getName()).isEqualTo("Sonar way");
    Set<String> keys = new HashSet<>();
    for (ActiveRule activeRule : activeRules) {
      keys.add(activeRule.getRuleKey());
    }
    //Check that we store active rule with legacy keys, not RSPEC keys
    assertThat(keys.contains("S116")).isFalse();
    assertThat(keys).contains("S00116");
    assertThat(validation.hasErrors()).isFalse();

    // Check that we use severity from the read rule and not default one.
    assertThat(activeRules.get(0).getSeverity()).isSameAs(RulePriority.MINOR);
  }


  @Test
  public void should_load_profile_in_sonar_lint_context() {
    ProfileDefinition sonarWay = new JavaSonarWayProfile(ruleFinder(true));
    ValidationMessages validation = ValidationMessages.create();
    RulesProfile profile = sonarWay.createProfile(validation);
    assertThat(profile.getActiveRules()).extracting("repositoryKey").containsOnly(CheckList.REPOSITORY_KEY);
    assertThat(profile.getActiveRulesByRepository("common-java")).isEmpty();
    assertThat(profile.getActiveRulesByRepository(CheckList.REPOSITORY_KEY).size()).isGreaterThanOrEqualTo(269);
    assertThat(profile.getName()).isEqualTo("Sonar way");

    assertThat(validation.hasErrors()).isFalse();
    // Check that we use severity from the read rule and not default one.
    assertThat(profile.getActiveRulesByRepository(CheckList.REPOSITORY_KEY).get(0).getSeverity()).isSameAs(RulePriority.MINOR);

  }

  static RuleFinder ruleFinder(boolean forSonarLint) {
    return when(mock(RuleFinder.class).findByKey(anyString(), anyString())).thenAnswer(new Answer<Rule>() {
      @Override
      public Rule answer(InvocationOnMock invocation) {
        Object[] arguments = invocation.getArguments();
        if(!forSonarLint || CheckList.REPOSITORY_KEY.equals(arguments[0])) {
          Rule rule = Rule.create((String) arguments[0], (String) arguments[1], (String) arguments[1]);
          return rule.setSeverity(RulePriority.MINOR);
        }
        return null;
      }
    }).getMock();
  }
}
