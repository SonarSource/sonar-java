/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

@Rule(key = "S5527")
public class VerifiedServerHostnamesCheck extends IssuableSubscriptionVisitor {

  private static final String JAVAX_NET_SSL_HOSTNAME_VERIFIER = "javax.net.ssl.HostnameVerifier";
  private static final String ISSUE_MESSAGE = "Enable server hostname verification on this SSL/TLS connection.";
  private static final MethodMatcher HOSTNAME_VERIFIER = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf(JAVAX_NET_SSL_HOSTNAME_VERIFIER))
    .name("verify")
    .parameters("java.lang.String", "javax.net.ssl.SSLSession");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      checkMethodDefinition((MethodTree) tree);
    } else {
      checkLambdaDefinition(((LambdaExpressionTree) tree));
    }
  }

  private void checkMethodDefinition(MethodTree tree) {
    BlockTree blockTree = tree.block();
    if (blockTree == null) {
      return;
    }
    if (HOSTNAME_VERIFIER.matches(tree)) {
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
    List<StatementTree> innerBlock = blockTree.body();
    while (innerBlock.size() == 1 && innerBlock.get(0).is(Tree.Kind.BLOCK)) {
      innerBlock = ((BlockTree) innerBlock.get(0)).body();
    }

    List<StatementTree> statementTreeList = innerBlock.stream()
      .filter(statementTree -> !statementTree.is(Tree.Kind.EMPTY_STATEMENT))
      .collect(Collectors.toList());
    if (isReturnTrueStatement(statementTreeList)) {
      reportIssue(statementTreeList.get(0), ISSUE_MESSAGE);
    }
  }

  private static boolean isHostnameVerifierSignature(LambdaExpressionTree lambdaExpressionTree) {
    return lambdaExpressionTree.symbolType().isSubtypeOf(JAVAX_NET_SSL_HOSTNAME_VERIFIER);
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
