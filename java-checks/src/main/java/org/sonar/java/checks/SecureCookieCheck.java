/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2092",
  name = "Cookies should be \"secure\"",
  tags = {"cwe", "owasp-top10", "security"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SECURITY_FEATURES)
@SqaleConstantRemediation("5min")
public class SecureCookieCheck extends SubscriptionBaseVisitor {

  private List<Symbol> unsecuredCookies = Lists.newArrayList();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    unsecuredCookies.clear();
    super.scanFile(context);
    for (Symbol unsecuredCookie : unsecuredCookies) {
      addIssue(getSemanticModel().getTree(unsecuredCookie), "Add the \"secure\" attribute to this cookie");
    }
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        Type type = ((AbstractTypedTree) variableTree.type()).getSymbolType();
        if (type.is("javax.servlet.http.Cookie") && isConstructorInitialized(variableTree)) {
          Symbol variableSymbol = getSemanticModel().getSymbol(variableTree);
          //Ignore field variables
          if (variableSymbol.owner().getType().isTagged(Type.METHOD)) {
            unsecuredCookies.add(variableSymbol);
          }
        }
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) tree;
        if (isSetSecureCall(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
          MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
          if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
            Symbol reference = getSemanticModel().getReference((IdentifierTree) mse.expression());
            unsecuredCookies.remove(reference);
          }
        }
      }
    }
  }

  private boolean isConstructorInitialized(VariableTree variableTree) {
    return variableTree.initializer() != null && variableTree.initializer().is(Tree.Kind.NEW_CLASS);
  }

  private boolean isSetSecureCall(MethodInvocationTreeImpl mit) {
    Symbol methodSymbol = mit.getSymbol();
    boolean hasArityOne = mit.arguments().size() == 1;
    if (hasArityOne && isCallSiteCookie(methodSymbol)) {
      ExpressionTree expressionTree = mit.arguments().get(0);
      if (expressionTree.is(Tree.Kind.BOOLEAN_LITERAL) && "false".equals(((LiteralTree) expressionTree).value())) {
        return false;
      }
      return "setSecure".equals(getIdentifier(mit).name());
    }
    return false;
  }

  private boolean isCallSiteCookie(Symbol methodSymbol) {
    return !methodSymbol.isKind(Symbol.ERRONEOUS) && methodSymbol.owner().getType().is("javax.servlet.http.Cookie");
  }

  private IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
