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
package org.sonar.plugins.java.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonar.java.TestUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class IssuableSubscriptionVisitorTest {

  @Test
  void test_custom_rules_report_issues() throws Exception {
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(Collections.singletonList(new CustomRule()), new ArrayList<>(), null);
    JavaAstScanner.scanSingleFileForTests(TestUtils.inputFile("src/test/resources/IssuableSubscriptionClass.java"), visitorsBridge);
    Set<AnalyzerMessage> issues = visitorsBridge.lastCreatedTestContext().getIssues();
    assertThat(issues).hasSize(7);
  }

  @Test
  void check_issuable_subscription_visitor_does_not_visit_tree_on_its_own() {
    CompilationUnitTree tree = Mockito.mock(CompilationUnitTree.class);
    CustomRule visitor = new CustomRule();
    try {
      visitor.scanTree(tree);
      fail("Analysis should have failed");
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("IssuableSubscriptionVisitor should not drive visit of AST.");
    }
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
