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
import com.google.common.collect.Lists;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(key = "S2092")
public class SecureCookieCheck extends IssuableSubscriptionVisitor {

  private List<Symbol.VariableSymbol> unsecuredCookies = Lists.newArrayList();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    unsecuredCookies.clear();
    super.scanFile(context);
    for (Symbol.VariableSymbol unsecuredCookie : unsecuredCookies) {
      reportIssue(unsecuredCookie.declaration().simpleName(), "Add the \"secure\" attribute to this cookie");
    }
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        addToUnsecuredCookies(variableTree);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) tree;
        checkSecureCall(mit);
      }
    }
  }

  private void addToUnsecuredCookies(VariableTree variableTree) {
    Type type = variableTree.type().symbolType();
    if (type.is("javax.servlet.http.Cookie") && isConstructorInitialized(variableTree)) {
      Symbol variableSymbol = variableTree.symbol();
      //Ignore field variables
      if (variableSymbol.isVariableSymbol() && variableSymbol.owner().isMethodSymbol()) {
        unsecuredCookies.add((Symbol.VariableSymbol) variableSymbol);
      }
    }
  }

  private void checkSecureCall(MethodInvocationTree mit) {
    if (isSetSecureCall(mit) && mit.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) mit.methodSelect();
      if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
        Symbol reference = ((IdentifierTree) mse.expression()).symbol();
        unsecuredCookies.remove(reference);
      }
    }
  }

  private static boolean isConstructorInitialized(VariableTree variableTree) {
    ExpressionTree initializer = variableTree.initializer();
    return initializer != null && initializer.is(Tree.Kind.NEW_CLASS);
  }

  private static boolean isSetSecureCall(MethodInvocationTree mit) {
    Symbol methodSymbol = mit.symbol();
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

  private static boolean isCallSiteCookie(Symbol methodSymbol) {
    return methodSymbol.isMethodSymbol() && methodSymbol.owner().type().is("javax.servlet.http.Cookie");
  }

  private static IdentifierTree getIdentifier(MethodInvocationTree mit) {
    IdentifierTree id;
    if (mit.methodSelect().is(Tree.Kind.IDENTIFIER)) {
      id = (IdentifierTree) mit.methodSelect();
    } else {
      id = ((MemberSelectExpressionTree) mit.methodSelect()).identifier();
    }
    return id;
  }
}
