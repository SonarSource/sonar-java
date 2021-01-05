/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.java.CheckTestUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostAnalysisIssueFilterTest {

  private static final InputFile INPUT_FILE = CheckTestUtils.inputFile("src/test/files/filters/PostAnalysisIssueFilter.java");
  private JavaFileScannerContext context;
  private PostAnalysisIssueFilter postAnalysisIssueFilter;
  private FilterableIssue fakeIssue;

  @BeforeEach
  public void setUp() {
    postAnalysisIssueFilter = new PostAnalysisIssueFilter();

    context = mock(JavaFileScannerContext.class);
    when(context.getInputFile()).thenReturn(INPUT_FILE);
    when(context.getSemanticModel()).thenReturn(new Object());

    fakeIssue = mock(FilterableIssue.class);
    when(fakeIssue.componentKey()).thenReturn("component");
    when(fakeIssue.ruleKey()).thenReturn(RuleKey.of("repo", "SXXXX"));
  }

  @Test
  void number_of_issue_filters() {
    assertThat(PostAnalysisIssueFilter.ISSUE_FILTERS).hasSize(5);
  }

  @Test
  void issue_filter_should_reject_issue_if_chain_reject_the_issue() {
    IssueFilterChain chain = mock(IssueFilterChain.class);
    when(chain.accept(ArgumentMatchers.any())).thenReturn(false);

    assertThat(postAnalysisIssueFilter.accept(fakeIssue, chain)).isFalse();
  }

  @Test
  void issue_filter_should_accept_issue_if_chain_accept_the_issue() {
    IssueFilterChain chain = mock(IssueFilterChain.class);
    when(chain.accept(ArgumentMatchers.any())).thenReturn(true);

    assertThat(postAnalysisIssueFilter.accept(fakeIssue, chain)).isTrue();
  }

  @Test
  void issue_filter_should_reject_issue_if_a_filter_rejects_the_issue() {
    IssueFilterChain chain = mock(IssueFilterChain.class);
    when(chain.accept(ArgumentMatchers.any())).thenReturn(true);

    when(fakeIssue.componentKey()).thenReturn(INPUT_FILE.key());
    when(fakeIssue.line()).thenReturn(42);

    postAnalysisIssueFilter.scanFile(context);

    InternalSyntaxToken fakeToken = new InternalSyntaxToken(42, 0, "fake_token", Collections.emptyList(), false);
    GeneratedCodeFilter filter = (GeneratedCodeFilter) PostAnalysisIssueFilter.ISSUE_FILTERS.get(4);
    filter.excludeLines(fakeToken);

    assertThat(postAnalysisIssueFilter.accept(fakeIssue, chain)).isFalse();
  }

  @Test
  void two_instances_of_PostAnalysisIssueFilter_share_filters() {
    IssueFilterChain chain = mock(IssueFilterChain.class);
    when(chain.accept(ArgumentMatchers.any())).thenReturn(true);

    when(fakeIssue.componentKey()).thenReturn(INPUT_FILE.key());
    when(fakeIssue.line()).thenReturn(666);

    PostAnalysisIssueFilter p1 = new PostAnalysisIssueFilter();
    PostAnalysisIssueFilter p2 = new PostAnalysisIssueFilter();

    p1.scanFile(context);
    p2.scanFile(context);

    assertThat(p1.accept(fakeIssue, chain)).isTrue();
    assertThat(p2.accept(fakeIssue, chain)).isTrue();

    InternalSyntaxToken fakeToken = new InternalSyntaxToken(666, 0, "fake_token", Collections.emptyList(), false);
    GeneratedCodeFilter filter = (GeneratedCodeFilter) PostAnalysisIssueFilter.ISSUE_FILTERS.get(4);
    filter.excludeLines(fakeToken);

    assertThat(p1.accept(fakeIssue, chain)).isFalse();
    assertThat(p2.accept(fakeIssue, chain)).isFalse();
  }

  @Test
  void issue_filter_should_scan_file_with_all_filters() {
    postAnalysisIssueFilter.scanFile(context);
    Mockito.verify(context, Mockito.times(5)).getInputFile();
  }

}
