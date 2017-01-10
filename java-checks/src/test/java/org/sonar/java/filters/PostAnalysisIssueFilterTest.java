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
package org.sonar.java.filters;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.squidbridge.api.AnalysisException;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostAnalysisIssueFilterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String FILE_KEY = "PostAnalysisIssueFilter.java";
  private static JavaFileScannerContext context;
  private PostAnalysisIssueFilter postAnalysisIssueFilter;
  private static final ArrayList<FakeJavaIssueFilter> ISSUE_FILTERS = Lists.<FakeJavaIssueFilter>newArrayList(new FakeJavaIssueFilter(true), new FakeJavaIssueFilter(false));

  @Before
  public void setUp() {
    DefaultFileSystem fileSystem = new DefaultFileSystem(new File("src/test/files/filters"));
    DefaultInputFile inputFile = new DefaultInputFile("", FILE_KEY);
    inputFile.setLanguage("java");
    inputFile.setType(InputFile.Type.MAIN);
    fileSystem.add(inputFile);
    postAnalysisIssueFilter = new PostAnalysisIssueFilter(fileSystem);

    context = mock(JavaFileScannerContext.class);
    when(context.getFile()).thenReturn(inputFile.file());
    when(context.getFileKey()).thenReturn(inputFile.key());
  }

  @Test
  public void number_of_issue_filters() {
    assertThat(postAnalysisIssueFilter.getIssueFilters()).hasSize(3);
  }

  @Test
  public void issue_filter_should_reject_issue_if_any_issue_filter_reject_the_issue() {
    postAnalysisIssueFilter.setIssueFilters(ISSUE_FILTERS);

    assertThat(postAnalysisIssueFilter.accept(mock(FilterableIssue.class), mock(IssueFilterChain.class))).isFalse();
  }

  @Test
  public void issue_filter_should_depends_on_chain_if_filters_accetps() {
    postAnalysisIssueFilter.setIssueFilters(new ArrayList<JavaIssueFilter>());

    FilterableIssue issue = mock(FilterableIssue.class);
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
      assertThat(filter.componentKey).isEqualTo(":PostAnalysisIssueFilter.java");
      assertThat(filter.scanned).isTrue();
    }
  }

  @Test
  public void missing_component_trigger_Exception() {
    thrown.expect(AnalysisException.class);

    postAnalysisIssueFilter.setIssueFilters(ISSUE_FILTERS);
    when(context.getFile()).thenReturn(new File(""));

    postAnalysisIssueFilter.scanFile(context);
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
    public boolean accept(FilterableIssue issue) {
      return accepted;
    }

    @Override
    public Set<Class<? extends JavaCheck>> filteredRules() {
      return null;
    }
  }

}
