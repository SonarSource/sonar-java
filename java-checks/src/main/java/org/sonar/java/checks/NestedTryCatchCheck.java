/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Rule(key = "S1141")
public class NestedTryCatchCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<Deque<Tree>> nestingLevel = new ArrayDeque<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    nestingLevel.clear();
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    nestingLevel.push(new ArrayDeque<>());
    super.visitClass(tree);
    nestingLevel.pop();
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    nestingLevel.push(new ArrayDeque<>());
    super.visitLambdaExpression(lambdaExpressionTree);
    nestingLevel.pop();
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resourceList());
    Deque<Tree> currentNestingLevel = nestingLevel.peek();

    if (!tree.catches().isEmpty()) {
      int size = currentNestingLevel.size();
      if (size > 0) {
        List<JavaFileScannerContext.Location> secondary = new ArrayList<>(size);
        for (Tree element : currentNestingLevel) {
          secondary.add(new JavaFileScannerContext.Location("Nesting + 1", element));
        }
        context.reportIssue(this, tree.tryKeyword(), "Extract this nested try block into a separate method.", secondary, null);
      }
      currentNestingLevel.push(tree.tryKeyword());
    }
    scan(tree.block());
    if (!tree.catches().isEmpty()) {
      currentNestingLevel.pop();
    }
    scan(tree.catches());
    scan(tree.finallyBlock());
  }
}
