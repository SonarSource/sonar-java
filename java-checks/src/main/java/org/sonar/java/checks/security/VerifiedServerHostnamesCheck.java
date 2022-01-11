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
package org.sonar.java.checks.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.ExpressionsHelper.getConstantValueAsString;
import static org.sonar.java.model.ExpressionUtils.extractIdentifierSymbol;
import static org.sonar.java.model.ExpressionUtils.isInvocationOnVariable;

@Rule(key = "S5527")
public class VerifiedServerHostnamesCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MESSAGE = "Enable server hostname verification on this SSL/TLS connection.";

  private static final String JAVAX_NET_SSL_HOSTNAME_VERIFIER = "javax.net.ssl.HostnameVerifier";
  private static final MethodMatchers HOSTNAME_VERIFIER = MethodMatchers.create()
    .ofSubTypes(JAVAX_NET_SSL_HOSTNAME_VERIFIER)
    .names("verify")
    .addParametersMatcher("java.lang.String", "javax.net.ssl.SSLSession")
    .build();

  private static final String APACHE_EMAIL = "org.apache.commons.mail.Email";
  private static final Set<String> ENABLING_SSL_METHOD_NAMES = new HashSet<>(Arrays.asList(
    "setSSL",
    "setSSLOnConnect",
    "setTLS",
    "setStartTLSEnabled",
    "setStartTLSRequired"));
  private static final MethodMatchers ENABLING_SSL_METHODS = MethodMatchers.create()
    .ofSubTypes(APACHE_EMAIL)
    .name(ENABLING_SSL_METHOD_NAMES::contains)
    .addParametersMatcher("boolean")
    .build();

  private static final MethodMatchers HASHTABLE_PUT = MethodMatchers.create()
    .ofSubTypes("java.util.Hashtable")
    .names("put")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case METHOD:
        checkMethodDefinition((MethodTree) tree);
        break;
      case LAMBDA_EXPRESSION:
        checkLambdaDefinition(((LambdaExpressionTree) tree));
        break;
      case METHOD_INVOCATION:
        checkMethodInvocation((MethodInvocationTree) tree);
        break;
      default:
        // can not happen
        break;
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

  private void checkMethodInvocation(MethodInvocationTree mit) {
    MethodTree method = ExpressionUtils.getEnclosingMethod(mit);
    if (method == null) {
      return;
    }

    ExpressionTree methodSelect = mit.methodSelect();

    if (!methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      return;
    }

    Symbol extractedIdentifier = extractIdentifierSymbol(((MemberSelectExpressionTree) methodSelect).expression()).orElse(null);

    if (ENABLING_SSL_METHODS.matches(mit) && LiteralUtils.isTrue(mit.arguments().get(0))) {
      MethodBodyApacheVisitor apacheVisitor = new MethodBodyApacheVisitor(extractedIdentifier);
      method.accept(apacheVisitor);
      if (!apacheVisitor.isSecured) {
        reportIssue(mit, ISSUE_MESSAGE);
      }
    } else if (HASHTABLE_PUT.matches(mit) && isSettingSSL(mit.arguments())) {
      MethodBodyHashtableVisitor hashtableVisitor = new MethodBodyHashtableVisitor(extractedIdentifier);
      method.accept(hashtableVisitor);
      if (!hashtableVisitor.isSecured) {
        reportIssue(mit, "Enable server hostname verification on this SSL/TLS connection, by setting \"mail.smtp.ssl.checkserveridentity\" to true.");
      }
    }
  }

  private static boolean isSettingSSL(Arguments args) {
    return "mail.smtp.socketFactory.class".equals(getConstantValueAsString(args.get(0)).value())
      && "javax.net.ssl.SSLSocketFactory".equals(getConstantValueAsString(args.get(1)).value());
  }

  private static boolean isNotFalse(ExpressionTree expression) {
    return !LiteralUtils.isFalse(expression);
  }

  private static class MethodBodyApacheVisitor extends BaseTreeVisitor {
    private boolean isSecured = false;
    private Symbol variable;

    private static final MethodMatchers SET_SSL_CHECK_SERVER_ID = MethodMatchers.create()
      .ofSubTypes(APACHE_EMAIL)
      .names("setSSLCheckServerIdentity")
      .addParametersMatcher("boolean")
      .build();

    MethodBodyApacheVisitor(@Nullable Symbol variable) {
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (isInvocationOnVariable(mit, variable, true) && SET_SSL_CHECK_SERVER_ID.matches(mit) && isNotFalse(mit.arguments().get(0))) {
        this.isSecured = true;
      }
      super.visitMethodInvocation(mit);
    }
  }

  private static class MethodBodyHashtableVisitor extends BaseTreeVisitor {
    private boolean isSecured = false;
    private Symbol variable;

    MethodBodyHashtableVisitor(@Nullable Symbol variable) {
      this.variable = variable;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (isInvocationOnVariable(mit, variable, true)) {
        Arguments args = mit.arguments();
        if (HASHTABLE_PUT.matches(mit)
          && "mail.smtp.ssl.checkserveridentity".equals(getConstantValueAsString(args.get(0)).value())
          && isNotFalse(args.get(1))) {
          this.isSecured = true;
        }
      }
      super.visitMethodInvocation(mit);
    }
  }
}
