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
package org.sonar.plugins.checkstyle;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.test.i18n.RuleRepositoryTestHelper;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CheckstyleRuleRepositoryTest {
  CheckstyleRuleRepository repository;

  @Before
  public void setUpRuleRepository() {
    repository = new CheckstyleRuleRepository(mock(ServerFileSystem.class), new XMLRuleParser());
  }

  @Test
  public void loadRepositoryFromXml() {
    List<Rule> rules = repository.createRules();

    assertThat(repository.getKey()).isEqualTo("checkstyle");
    assertThat(rules.size()).isEqualTo(128);
  }

  @Test
  public void should_provide_a_name_and_description_for_each_rule() {
    List<Rule> rules = RuleRepositoryTestHelper.createRulesWithNameAndDescription("checkstyle", repository);

    assertThat(rules).onProperty("name").excludes(null, "");
    assertThat(rules).onProperty("description").excludes(null, "");
  }
}
