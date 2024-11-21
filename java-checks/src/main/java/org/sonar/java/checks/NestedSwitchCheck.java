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
package org.sonar.java.checks;

import java.util.Arrays;
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
    return Arrays.asList(Tree.Kind.SWITCH_STATEMENT, Tree.Kind.SWITCH_EXPRESSION);
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
