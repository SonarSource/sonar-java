/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.plugins.java.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.testing.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuableSubscriptionVisitorTest {

  @Test
  void test_custom_rules_report_issues() {
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(Collections.singletonList(new CustomRule()), new ArrayList<>(), null, new JavaVersionImpl());
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/resources/IssuableSubscriptionClass.java"), visitorsBridge);
    Set<AnalyzerMessage> issues = visitorsBridge.lastCreatedTestContext().getIssues();
    assertThat(issues).hasSize(7);
  }

  @Test
  void check_issuable_subscription_visitor_does_not_visit_tree_on_its_own() {
    CompilationUnitTree tree = Mockito.mock(CompilationUnitTree.class);
    CustomRule visitor = new CustomRule();
    assertThatThrownBy(() -> visitor.scanTree(tree))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssuableSubscriptionVisitor should not drive visit of AST.");
  }

  @Test
  void issuable_subscription_visitor_does_not_visit_file_on_its_own() {
    CustomRule visitor = new CustomRule();
    JavaFileScannerContext context = Mockito.mock(JavaFileScannerContext.class);
    assertThatThrownBy(() -> visitor.scanFile(context))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("IssuableSubscriptionVisitor should not drive visit of file. Use leaveFile() instead.");
  }

  private static class CustomRule extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return Collections.singletonList(Tree.Kind.COMPILATION_UNIT);
    }

    @Override
    public void visitNode(Tree tree) {
      reportIssue(tree, "issue on tree");
      addIssue(1, "issue on 1st line");
      reportIssue(tree, "message", new ArrayList<>(), 0);
      addIssueOnFile("issue on file");
      reportIssue(tree, "issue on tree");
      reportIssue(tree, "issue on tree", Collections.emptyList(), null);
      reportIssue(tree, tree, "issue from tree to tree");
    }
  }
}
