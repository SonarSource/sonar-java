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
package org.sonar.plugins.java.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.model.VisitorsBridgeForTests;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuableSubscriptionVisitorTest {

  @Test
  public void test_custom_rules_report_issues() throws Exception {
    VisitorsBridgeForTests visitorsBridge = new VisitorsBridgeForTests(Lists.newArrayList(new CustomRule()), Lists.<File>newArrayList(), null);
    JavaAstScanner.scanSingleFileForTests(new File("src/test/resources/IssuableSubscriptionClass.java"), visitorsBridge);
    Set<AnalyzerMessage> issues = visitorsBridge.lastCreatedTestContext().getIssues();
    assertThat(issues).hasSize(7);
  }

  private static class CustomRule extends IssuableSubscriptionVisitor {

    @Override
    public List<Tree.Kind> nodesToVisit() {
      return ImmutableList.of(Tree.Kind.COMPILATION_UNIT);
    }

    @Override
    public void visitNode(Tree tree) {
      reportIssue(tree, "issue on tree");
      addIssue(1, "issue on 1st line");
      reportIssue(tree, "message", Lists.<JavaFileScannerContext.Location>newArrayList(), 0);
      addIssueOnFile("issue on file");
      reportIssue(tree, "issue on tree");
      reportIssue(tree, "issue on tree", ImmutableList.<JavaFileScannerContext.Location>of(), null);
      reportIssue(tree, tree, "issue from tree to tree");
    }
  }
}
