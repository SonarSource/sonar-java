/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SwitchTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1821")
public class NestedSwitchCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    NestedSwitchVisitor visitor = new NestedSwitchVisitor();
    ((SwitchTree) tree).cases()
      .forEach(c -> c.accept(visitor));
  }

  private class NestedSwitchVisitor extends BaseTreeVisitor {

    @Override
    public void visitClass(ClassTree tree) {
      // skip nested and anonymous Classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip Lambdas
    }

    @Override
    public void visitSwitchStatement(SwitchStatementTree tree) {
      reportNestedSwitch(tree);
    }

    @Override
    public void visitSwitchExpression(SwitchExpressionTree tree) {
      reportNestedSwitch(tree);
    }

    private void reportNestedSwitch(SwitchTree switchTree) {
      reportIssue(switchTree.switchKeyword(), "Refactor the code to eliminate this nested \"switch\".");
    }
  }
}
