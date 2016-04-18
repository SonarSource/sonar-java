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
package org.sonar.java.filters;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.java.bytecode.visitor.ResourceMapping;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.ArrayList;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostAnalysisIssueFilterTest {

  private static final String COMPONENT_KEY = "my_component";
  private static final String FILE_KEY = "my_file";
  private static JavaFileScannerContext context;
  private PostAnalysisIssueFilter postAnalysisIssueFilter;
  private static final ArrayList<FakeJavaIssueFilter> ISSUE_FILTERS = Lists.<FakeJavaIssueFilter>newArrayList(new FakeJavaIssueFilter(true), new FakeJavaIssueFilter(false));

  @Before
  public void setUp() {
    postAnalysisIssueFilter = new PostAnalysisIssueFilter();
    ResourceMapping resourceMapping = mock(ResourceMapping.class);
    when(resourceMapping.getComponentKeyByFileKey(anyString())).thenReturn(COMPONENT_KEY);
    postAnalysisIssueFilter.setResourceMapping(resourceMapping);

    context = mock(JavaFileScannerContext.class);
    when(context.getFileKey()).thenReturn(FILE_KEY);
  }

  @Test
  public void number_of_issue_filters() {
    assertThat(postAnalysisIssueFilter.getIssueFilters()).hasSize(3);
  }

  @Test
  public void issue_filter_should_reject_issue_if_any_issue_filter_reject_the_issue() {
    postAnalysisIssueFilter.setIssueFilters(ISSUE_FILTERS);

    assertThat(postAnalysisIssueFilter.accept(mock(Issue.class), mock(IssueFilterChain.class))).isFalse();
  }

  @Test
  public void issue_filter_should_depends_on_chain_if_filters_accetps() {
    postAnalysisIssueFilter.setIssueFilters(new ArrayList<JavaIssueFilter>());

    Issue issue = mock(Issue.class);
    IssueFilterChain chain = mock(IssueFilterChain.class);

    when(chain.accept(issue)).thenReturn(true);
    assertThat(postAnalysisIssueFilter.accept(issue, chain)).isTrue();

    when(chain.accept(issue)).thenReturn(false);
    assertThat(postAnalysisIssueFilter.accept(issue, chain)).isFalse();
  }

  @Test
  public void issue_filter_should_set_componentKey_and_scan_every_filter() {
    postAnalysisIssueFilter.setIssueFilters(ISSUE_FILTERS);
    postAnalysisIssueFilter.scanFile(context);

    for (FakeJavaIssueFilter filter : ISSUE_FILTERS) {
      assertThat(filter.componentKey).isEqualTo(COMPONENT_KEY);
      assertThat(filter.scanned).isTrue();
    }
  }

  private static class FakeJavaIssueFilter implements JavaIssueFilter {

    private final boolean accepted;
    private boolean scanned = false;
    private String componentKey;

    FakeJavaIssueFilter(boolean accept) {
      this.accepted = accept;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      scanned = true;
    }

    @Override
    public void setComponentKey(String componentKey) {
      this.componentKey = componentKey;
    }

    @Override
    public boolean accept(Issue issue) {
      return accepted;
    }

    @Override
    public Set<Class<? extends JavaCheck>> filteredRules() {
      return null;
    }
  }

}
