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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.fest.assertions.BooleanAssert;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseTreeVisitorIssueFilterTest {

  private static final String REPOSITORY_KEY = "octopus";
  private static final String COMPONENT_KEY = "test:test.MyTest";
  private static final String FILE_KEY = "src/test/files/filter/BaseTreeVisitorIssueFilter.java";
  private static final String RULE_KEY = "S42";
  private BaseTreeVisitorIssueFilter filter;
  private Issue issue;

  @Before
  public void setup() {
    issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn(COMPONENT_KEY);
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, RULE_KEY));

    filter = new FakeJavaIssueFilterOnClassAndVariable();

    filter.setComponentKey(COMPONENT_KEY);
    scanFile(filter);
  }

  @Test
  public void issues_by_targeted_rule_should_be_filtered() {
    // issue on file
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class accepted
    assertThatIssueWillBeAccepted(3).isTrue();

    // issue on variable filtered
    assertThatIssueWillBeAccepted(4).isFalse();
    assertThatIssueWillBeAccepted(5).isFalse();
  }

  @Test
  public void issues_from_non_targeted_rules_are_accepted() {
    // other rule
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule"));

    // issue on file accepted
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class accepted
    assertThatIssueWillBeAccepted(3).isTrue();

    // issue on variable accepted
    assertThatIssueWillBeAccepted(4).isTrue();
    assertThatIssueWillBeAccepted(5).isTrue();
  }

  @Test
  public void issues_from_other_component_are_accepted() {
    // targeted rule
    when(issue.componentKey()).thenReturn("UnknownComponent");

    // issue on file accepted
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class accepted
    assertThatIssueWillBeAccepted(3).isTrue();

    // issue on variable accepted
    assertThatIssueWillBeAccepted(4).isTrue();
    assertThatIssueWillBeAccepted(5).isTrue();
  }

  @Test
  public void filter_do_not_suppress_lines_on_unknown_components() {
    // component is not added
    filter = new FakeJavaIssueFilterOnClassAndVariable();
    scanFile(filter);

    // issue on file accepted
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on class accepted
    assertThatIssueWillBeAccepted(3).isTrue();

    // issue on variable accepted
    assertThatIssueWillBeAccepted(4).isTrue();
    assertThatIssueWillBeAccepted(5).isTrue();
  }

  private BooleanAssert assertThatIssueWillBeAccepted(@Nullable Integer line) {
    when(issue.line()).thenReturn(line);
    return assertThat(filter.accept(issue));
  }

  private static class FakeJavaIssueFilterOnClassAndVariable extends BaseTreeVisitorIssueFilter {
    @Override
    public Set<String> targetedRules() {
      return ImmutableSet.of(RULE_KEY);
    }

    @Override
    public void visitVariable(VariableTree tree) {
      ignoreIssuesInTree(tree);
      super.visitVariable(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      IdentifierTree simpleName = tree.simpleName();
      if (simpleName == null) {
        // force check on null tree
        ignoreIssuesInTree(simpleName);
      }
      super.visitClass(tree);
    }
  }

  private static void scanFile(JavaIssueFilter filter) {
    List<JavaFileScanner> visitors = Lists.<JavaFileScanner>newArrayList(filter);
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(visitors, Lists.<File>newLinkedList(), null);
    JavaAstScanner.scanSingleFileForTests(new File(FILE_KEY), visitorsBridge);
  }
}
