/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.filters;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.CheckTestUtils;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostAnalysisIssueFilterTest {

  private static final InputFile INPUT_FILE = CheckTestUtils.inputFile("src/test/files/filters/PostAnalysisIssueFilter.java");
  private static JavaFileScannerContext context;
  private PostAnalysisIssueFilter postAnalysisIssueFilter;
  private static final FakeJavaIssueFilter acceptingIssueFilter = new FakeJavaIssueFilter(true);
  private static final ArrayList<FakeJavaIssueFilter> ISSUE_FILTERS = Lists.newArrayList(acceptingIssueFilter, new FakeJavaIssueFilter(false));

  @BeforeEach
  public void setUp() {
    postAnalysisIssueFilter = new PostAnalysisIssueFilter();

    context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(INPUT_FILE);
  }

  @Test
  void number_of_issue_filters() {
    assertThat(postAnalysisIssueFilter.getIssueFilters()).hasSize(5);
  }

  @Test
  void issue_filter_should_accept_issue() {
    postAnalysisIssueFilter.setIssueFilters(Lists.newArrayList(acceptingIssueFilter));
    assertThat(postAnalysisIssueFilter.accept(null, null)).isTrue();
  }

  @Test
  void issue_filter_should_reject_issue_if_any_issue_filter_reject_the_issue() {
    postAnalysisIssueFilter.setIssueFilters(ISSUE_FILTERS);
    assertThat(postAnalysisIssueFilter.accept(null, null)).isFalse();
  }

  @Test
  void issue_filter_should_set_componentKey_and_scan_every_filter() {
    postAnalysisIssueFilter.setIssueFilters(ISSUE_FILTERS);
    postAnalysisIssueFilter.scanFile(context);

    for (FakeJavaIssueFilter filter : ISSUE_FILTERS) {
      assertThat(filter.scanned).isTrue();
    }
  }

  private static class FakeJavaIssueFilter implements JavaIssueFilter {

    private final boolean accepted;
    private boolean scanned = false;

    FakeJavaIssueFilter(boolean accept) {
      this.accepted = accept;
    }

    @Override
    public void scanFile(JavaFileScannerContext context) {
      scanned = true;
    }

    @Override
    public boolean accept(RuleKey ruleKey, AnalyzerMessage analyzerMessage) {
      return accepted;
    }

    @Override
    public Set<Class<? extends JavaCheck>> filteredRules() {
      return null;
    }
  }

}
