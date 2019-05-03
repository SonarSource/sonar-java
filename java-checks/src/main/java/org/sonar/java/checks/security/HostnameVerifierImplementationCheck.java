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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3510")
public class HostnameVerifierImplementationCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Do not unconditionally return true in this method.";

  private static final TypeCriteria TYPE_CRITERIA_STRING = TypeCriteria.is("java.lang.String");
  private static final TypeCriteria TYPE_CRITERIA_SSL_SESSION = TypeCriteria.is("javax.net.ssl.SSLSession");
  private static final MethodMatcher VERIFY_METHOD_MATCHER = MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("javax.net.ssl.HostnameVerifier"))
    .name("verify")
    .addParameter(TYPE_CRITERIA_STRING)
    .addParameter(TYPE_CRITERIA_SSL_SESSION);

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    if (tree.kind().equals(Tree.Kind.METHOD)) {
      checkMethodDefinition((MethodTree) tree);
    } else {
      checkLambdaDefinition(((LambdaExpressionTree) tree));
    }
  }

  private void checkMethodDefinition(MethodTree tree) {
    BlockTree blockTree = tree.block();
    if (VERIFY_METHOD_MATCHER.matches(tree) && blockTree != null) {
      checkBlock(blockTree);
    }
  }

  private void checkLambdaDefinition(LambdaExpressionTree lambdaExpression) {
    Tree lambdaBody = lambdaExpression.body();
    if (isHostnameVerifierSignature(lambdaExpression)) {
      if (lambdaBody.is(Tree.Kind.BLOCK)) {
        checkBlock((BlockTree) lambdaBody);
      } else if (isTrueLiteral(lambdaBody)) {
        reportIssue(lambdaBody, ISSUE_MESSAGE);
      }
    }
  }

  private void checkBlock(BlockTree blockTree) {
    BlockTree innerBlock = blockTree;
    while (innerBlock.body().size() == 1 && innerBlock.body().get(0).is(Tree.Kind.BLOCK)) {
      innerBlock = (BlockTree) innerBlock.body().get(0);
    }

    List<StatementTree> statementTreeList = innerBlock.body().stream()
      .filter(statementTree -> !statementTree.is(Tree.Kind.EMPTY_STATEMENT))
      .collect(Collectors.toList());
    if (isReturnTrueStatement(statementTreeList)) {
      reportIssue(statementTreeList.get(0), ISSUE_MESSAGE);
    }
  }

  private static boolean isHostnameVerifierSignature(LambdaExpressionTree lambdaExpressionTree) {
    return lambdaExpressionTree.symbolType().isSubtypeOf("javax.net.ssl.HostnameVerifier");
  }

  private static boolean isReturnTrueStatement(List<StatementTree> statementTreeList) {
    if (statementTreeList.size() == 1 && statementTreeList.get(0).is(Tree.Kind.RETURN_STATEMENT)) {
      ExpressionTree expression = ((ReturnStatementTree) statementTreeList.get(0)).expression();
      return isTrueLiteral(expression);
    }
    return false;
  }

  private static boolean isTrueLiteral(Tree tree) {
    if (tree.is(Tree.Kind.PARENTHESIZED_EXPRESSION) || tree.is(Tree.Kind.BOOLEAN_LITERAL)) {
      ExpressionTree expression = ExpressionUtils.skipParentheses((ExpressionTree) tree);
      return LiteralUtils.isTrue(expression);
    }
    return false;
  }
}
