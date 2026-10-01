/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.check.Rule;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1602")
public class LambdaSingleExpressionCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final MethodMatchers SPRING_JDBC_QUERY_MATCHER = MethodMatchers.create()
    .ofSubTypes(
      "org.springframework.jdbc.core.JdbcOperations",
      "org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations")
    .names("query")
    .withAnyParameters()
    .build();

  private static final MethodMatchers METHOD_HANDLE_INVOKE_MATCHER = MethodMatchers.create()
    .ofTypes("java.lang.invoke.MethodHandle")
    .names("invoke", "invokeExact")
    .withAnyParameters()
    .build();

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
    if (isBlockWithOneStatement(lambdaBody)
      && !hasMultilineBody(lambdaExpressionTree)
      && !isInsideSpringJdbcQuery(lambdaExpressionTree)
      && !isSingleMethodHandleInvocation(lambdaExpressionTree)) {
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

  private static boolean hasMultilineBody(LambdaExpressionTree lambda) {
    BlockTree block = (BlockTree) lambda.body();
    return LineUtils.startLine(block.openBraceToken()) != LineUtils.startLine(block.closeBraceToken());
  }

  private static boolean isInsideSpringJdbcQuery(LambdaExpressionTree lambda) {
    if (lambda.parameters().size() != 1) {
      return false;
    }
    Tree parent = lambda.parent();
    if (parent != null && parent.is(Tree.Kind.ARGUMENTS)) {
      parent = parent.parent();
    }
    return parent != null && parent.is(Tree.Kind.METHOD_INVOCATION)
      && SPRING_JDBC_QUERY_MATCHER.matches((MethodInvocationTree) parent);
  }

  private static boolean isSingleMethodHandleInvocation(LambdaExpressionTree lambda) {
    StatementTree statement = ((BlockTree) lambda.body()).body().get(0);
    ExpressionTree expression = null;
    if (statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      expression = ((ExpressionStatementTree) statement).expression();
    } else if (statement.is(Tree.Kind.RETURN_STATEMENT)) {
      expression = ((ReturnStatementTree) statement).expression();
    }
    return expression != null && expression.is(Tree.Kind.METHOD_INVOCATION)
      && METHOD_HANDLE_INVOKE_MATCHER.matches((MethodInvocationTree) expression);
  }
}
