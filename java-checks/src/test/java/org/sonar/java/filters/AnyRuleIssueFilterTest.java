/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.filters;

import java.util.Collections;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class AnyRuleIssueFilterTest {

  private static final InputFile INPUT_FILE = TestUtils.inputFile(mainCodeSourcesPath("filters/AnyRuleIssueFilter.java"));
  private static final String REPOSITORY_KEY = "walrus";
  private static final String RULE_KEY = "S42";
  private AnyRuleIssueFilter filter;
  private FilterableIssue issue;

  @BeforeEach
  void setup() {
    issue = mock(FilterableIssue.class);
    when(issue.componentKey()).thenReturn(INPUT_FILE.key());
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, RULE_KEY));

    filter = new AnyRuleOnVariableIssueFilter();

    scanFile(filter);
  }

  @Test
  void any_rule_filter_does_not_requires_rules() {
    assertThat(new AnyRuleIssueFilter() {
    }.filteredRules()).isEmpty();
  }

  @Test
  void issue_on_other_component_are_ignored() {
    when(issue.componentKey()).thenReturn("tesT:test.MyOtherTest");
    assertThatIssueWillBeAccepted(2).isTrue();
  }

  @Test
  void invalid_tree_does_not_exclude_lines() {

    // by default, any issue at line 7 is accepted
    assertThatIssueWillBeAccepted(7).isTrue();

    Tree mockTree = mock(Tree.class);
    // without first nor last token, line can not be excluded
    filter.excludeLines(mockTree);
    assertThatIssueWillBeAccepted(7).isTrue();

    SyntaxToken mockFirstToken = mock(SyntaxToken.class);
    when(mockFirstToken.range()).thenReturn(Range.at(7,1,7,2));
    when(mockTree.firstToken()).thenReturn(mockFirstToken);
    // without last token, line can not be excluded
    filter.excludeLines(mockTree);
    assertThatIssueWillBeAccepted(7).isTrue();

    SyntaxToken mockLastToken = mock(SyntaxToken.class);
    when(mockLastToken.range()).thenReturn(Range.at(7,1,7,2));
    when(mockTree.lastToken()).thenReturn(mockLastToken);
    // with first and last token, line 7 can be excluded
    filter.excludeLines(mockTree);
    assertThatIssueWillBeAccepted(7).isFalse();
  }

  @Test
  void issues_from_any_rules_are_accepted() {
    // issue on file accepted
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule1"));
    assertThatIssueWillBeAccepted(null).isTrue();

    // issue on field called 'field' rejected
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule2"));
    assertThatIssueWillBeAccepted(4).isFalse();

    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule3"));
    assertThatIssueWillBeAccepted(5).isFalse();

    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule4"));
    assertThatIssueWillBeAccepted(6).isFalse();

    // issue on other variables are accepted
    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule5"));
    assertThatIssueWillBeAccepted(8).isTrue();

    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule6"));
    assertThatIssueWillBeAccepted(9).isTrue();

    when(issue.ruleKey()).thenReturn(RuleKey.of(REPOSITORY_KEY, "OtherRule7"));
    // issue on trivia from the field
    assertThatIssueWillBeAccepted(12).isFalse();
    // issue on field
    assertThatIssueWillBeAccepted(14).isFalse();
  }

  private AbstractBooleanAssert<?> assertThatIssueWillBeAccepted(@Nullable Integer line) {
    when(issue.line()).thenReturn(line);
    return assertThat(filter.accept(issue));
  }

  private static void scanFile(JavaIssueFilter filter) {
    VisitorsBridgeForTests visitorsBridge =
      new VisitorsBridgeForTests.Builder(filter)
        .enableSemanticWithProjectClasspath(Collections.emptyList())
        .build();
    JavaAstScanner.scanSingleFileForTests(INPUT_FILE, visitorsBridge);
  }

  private static class AnyRuleOnVariableIssueFilter extends AnyRuleIssueFilter {

    @Override
    public void visitVariable(VariableTree tree) {
      // filter issues on variable with name starting by "field"
      if (tree.simpleName().identifierToken().text().toLowerCase().startsWith("field")) {
        excludeLines(tree);
      }
      super.visitVariable(tree);
    }
  }

}
