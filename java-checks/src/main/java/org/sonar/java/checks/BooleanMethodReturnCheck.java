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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(key = "S2447")
public class BooleanMethodReturnCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    if (!isAnnotatedWithCheckForNull(methodTree) && returnsBoolean(methodTree)) {
      methodTree.accept(new ReturnStatementVisitor());
    }
  }

  private static boolean isAnnotatedWithCheckForNull(MethodTree methodTree) {
    return methodTree.symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull");
  }

  private static boolean returnsBoolean(MethodTree methodTree) {
    return methodTree.returnType().symbolType().is("java.lang.Boolean");
  }

  private class ReturnStatementVisitor extends BaseTreeVisitor {

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (tree.expression().is(Kind.NULL_LITERAL)) {
        reportIssue(tree.expression(), "Null is returned but a \"Boolean\" is expected.");
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes as methods of inner classes will be visited by main visitor
    }
  }

}
