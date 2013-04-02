/*
 * Sonar Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.pmd;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.utils.ValidationMessages;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PmdProfilesTest {

  ValidationMessages validation = ValidationMessages.create();

  @Test
  public void should_create_sonar_way_profile() {
    ProfileDefinition sonarWay = new SonarWayProfile(new PmdProfileImporter(ruleFinder()));

    RulesProfile profile = sonarWay.createProfile(validation);

    assertThat(profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY)).isNotEmpty();
    assertThat(validation.hasErrors()).isFalse();
  }

  @Test
  public void should_create_sonar_way_with_findbugs_profile() {
    ProfileDefinition sonarWayWithFindbugs = new SonarWayWithFindbugsProfile(new SonarWayProfile(new PmdProfileImporter(ruleFinder())));

    RulesProfile profile = sonarWayWithFindbugs.createProfile(validation);

    assertThat(profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY)).isNotEmpty();
    assertThat(validation.hasErrors()).isFalse();
  }

  static RuleFinder ruleFinder() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.find(any(RuleQuery.class))).then(new Answer<Rule>() {
      public Rule answer(InvocationOnMock invocation) {
        RuleQuery query = (RuleQuery) invocation.getArguments()[0];
        return Rule.create(query.getRepositoryKey(), "", "");
      }
    });
    return ruleFinder;
  }

}
