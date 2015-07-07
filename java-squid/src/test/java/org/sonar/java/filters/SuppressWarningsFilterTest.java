/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.filters;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.rule.RuleKey;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuppressWarningsFilterTest {

  private static final String COMPONENT_KEY = "test:test.MyTest";
  IssueFilterChain chain = mock(IssueFilterChain.class);
  SuppressWarningsFilter filter = new SuppressWarningsFilter();

  @Before
  public void setupChain() {
    when(chain.accept(isA(Issue.class))).thenReturn(true);
  }

  @Test
  public void should_ignore_issue_if_out_of_scope() {
    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "firstIssue"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "squid:secondIssue", 12, 16);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    // issue on file
    when(issue.line()).thenReturn(null);
    assertTrue(filter.accept(issue, chain));

    // issue on not flagged line
    when(issue.line()).thenReturn(21);
    assertTrue(filter.accept(issue, chain));
  }

  @Test
  public void should_ignore_issue_if_in_other_component() {
    Issue issue = mock(Issue.class);

    // issue in other component
    when(issue.componentKey()).thenReturn(COMPONENT_KEY + "2");
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "firstIssue"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "squid:secondIssue", 12, 16);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    // issue on file
    when(issue.line()).thenReturn(null);
    assertTrue(filter.accept(issue, chain));

    // issue on not flagged line
    when(issue.line()).thenReturn(21);
    assertTrue(filter.accept(issue, chain));
  }

  @Test
  public void should_ignore_issue_if_same_rule_explicitly_mentioned_as_warning_parameter() {
    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of("pmd", "CloseResource"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "pmd:CloseResource", 12, 16);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    // issue on every line covered by @SuppressWarnings, but same as explicitly mentioned
    for (int i = 12; i <= 16; i++) {
      when(issue.line()).thenReturn(i);
      assertFalse(filter.accept(issue, chain));
    }
  }

  @Test
  public void should_accept_issue_if_different_rule_explicitly_mentioned_as_warning_parameter() {
    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of("pmd", "CloseResource"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "pmd:OtherIssue", 12, 16);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    // issue on line covered by @SuppressWarnings, but different from the one explicitly mentioned
    when(issue.line()).thenReturn(15);
    assertTrue(filter.accept(issue, chain));
  }

  @Test
  public void should_accept_issue_if_suppressWarning_rule_enabled_with_warning_is_all() {
    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "S1309"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "all", 12, 16);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    // issue on line covered by @SuppressWarnings
    when(issue.line()).thenReturn(12);
    assertTrue(filter.accept(issue, chain));
  }

  @Test
  public void should_ignore_issue_if_suppressWarning_rule_enabled_but_explicitly_hidden() {
    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "S1309"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "unchecked", 12, 16);
    addWarning(suppressWarningLines, "cast", 13, 15);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    // issue on line covered by @SuppressWarnings
    when(issue.line()).thenReturn(12);
    assertTrue(filter.accept(issue, chain));

    when(issue.line()).thenReturn(14);
    assertTrue(filter.accept(issue, chain));
  }

  @Test
  public void should_ignore_issue_if_suppressWarning_rule_not_enabled() {
    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "firstIssue"));

    Multimap<Integer, String> suppressWarningLines = HashMultimap.create();
    addWarning(suppressWarningLines, "all", 12, 16);

    filter.addComponent(COMPONENT_KEY, suppressWarningLines);

    when(issue.line()).thenReturn(12);
    assertFalse(filter.accept(issue, chain));
  }

  private void addWarning(Multimap<Integer, String> multimap, String warning, int startLine, int endLine) {
    for (int i = startLine; i <= endLine; i++) {
      multimap.put(i, warning);
    }
  }
}
