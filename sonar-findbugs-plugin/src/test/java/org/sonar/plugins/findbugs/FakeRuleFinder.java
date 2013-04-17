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
package org.sonar.plugins.findbugs;

import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.XMLRuleParser;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.mock;

public class FakeRuleFinder implements RuleFinder {

  private final List<Rule> findbugsRules;

  public FakeRuleFinder() {
    ServerFileSystem sfs = mock(ServerFileSystem.class);
    FindbugsRuleRepository repo = new FindbugsRuleRepository(sfs, new XMLRuleParser());
    findbugsRules = repo.createRules();
    for (Rule rule : findbugsRules) {
      rule.setRepositoryKey(FindbugsConstants.REPOSITORY_KEY);
    }
  }

  public Rule findById(int ruleId) {
    throw new UnsupportedOperationException();
  }

  public Rule findByKey(String repositoryKey, String key) {
    for (Rule rule : findbugsRules) {
      if (rule.getKey().equals(key)) {
        return rule;
      }
    }
    return null;
  }

  public Rule findByKey(RuleKey key) {
    return findByKey(key.repository(), key.rule());
  }

  public Rule find(RuleQuery query) {
    throw new UnsupportedOperationException();
  }

  public Collection<Rule> findAll(RuleQuery query) {
    return findbugsRules;
  }

}
