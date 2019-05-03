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
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1602")
public class LambdaSingleExpressionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) tree;
    Tree lambdaBody = lambdaExpressionTree.body();
    if (isBlockWithOneStatement(lambdaBody)) {
      String message = "Remove useless curly braces around statement";
      if (singleStatementIsReturn(lambdaExpressionTree)) {
        message += " and then remove useless return keyword";
      }
      reportIssue(((BlockTree) lambdaBody).openBraceToken(), message + context.getJavaVersion().java8CompatibilityMessage());
    }
  }

  private static boolean isBlockWithOneStatement(Tree tree) {
    boolean result = false;
    if (tree.is(Tree.Kind.BLOCK)) {
      List<StatementTree> blockBody = ((BlockTree) tree).body();
      result = blockBody.size() == 1 && isRefactorizable(blockBody.get(0));
    }
    return result;
  }

  private static boolean isRefactorizable(StatementTree statementTree) {
    return isBlockWithOneStatement(statementTree) || statementTree.is(Tree.Kind.EXPRESSION_STATEMENT) || isReturnStatement(statementTree);
  }

  private static boolean singleStatementIsReturn(LambdaExpressionTree lambdaExpressionTree) {
    return isReturnStatement(((BlockTree) lambdaExpressionTree.body()).body().get(0));
  }

  private static boolean isReturnStatement(Tree tree) {
    return tree.is(Tree.Kind.RETURN_STATEMENT);
  }
}
