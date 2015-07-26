/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S2077",
  name = "Values passed to SQL commands should be sanitized",
  tags = {"cwe", "owasp-a1", "sans-top25-insecure", "security", "sql", "hibernate"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INPUT_VALIDATION_AND_REPRESENTATION)
@SqaleConstantRemediation("20min")
public class SQLInjectionCheck extends AbstractInjectionChecker {

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodTree = (MethodInvocationTree) tree;
    boolean isHibernateCall = isHibernateCall(methodTree);
    if (isHibernateCall(methodTree) || isExecuteQueryOrPrepareStatement(methodTree)) {
      //We want to check the argument for the three methods.
      ExpressionTree arg = methodTree.arguments().get(0);
      parameterName = "";
      if (isDynamicString(methodTree, arg, null, true)) {
        String message = "\"" + parameterName + "\" is provided externally to the method and not sanitized before use.";
        if (isHibernateCall) {
          message = "Use Hibernate's parameter binding instead of concatenation.";
        }
        addIssue(methodTree, message);
      }
    }
  }

  private static boolean isExecuteQueryOrPrepareStatement(MethodInvocationTree methodTree) {
    if (methodTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodTree.methodSelect();
      return !methodTree.arguments().isEmpty() && (isMethodCall("java.sql.Statement", "executeQuery", memberSelectExpressionTree)
          || isMethodCall("java.sql.Connection", "prepareStatement", memberSelectExpressionTree)
          || isMethodCall("java.sql.Connection", "prepareCall", memberSelectExpressionTree)
      );
    }
    return false;
  }

  private static boolean isHibernateCall(MethodInvocationTree methodTree) {
    if (methodTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) methodTree.methodSelect();
      return !methodTree.arguments().isEmpty() && isMethodCall("org.hibernate.Session", "createQuery", memberSelectExpressionTree);
    }
    return false;
  }

  private static boolean isMethodCall(String typeName, String methodName, MemberSelectExpressionTree memberSelectExpressionTree) {
    return methodName.equals(memberSelectExpressionTree.identifier().name()) && isInvokedOnType(typeName, memberSelectExpressionTree.expression());
  }

  private static boolean isInvokedOnType(String type, ExpressionTree expressionTree) {
    Type selectorType = expressionTree.symbolType();
    if (selectorType.isClass()) {
      return type.equals(selectorType.fullyQualifiedName()) || checkInterfaces(type, selectorType.symbol());
    }
    return false;
  }

  private static boolean checkInterfaces(String type, Symbol.TypeSymbol symbol) {
    for (Type interfaceType : symbol.interfaces()) {
      if (type.equals(interfaceType.fullyQualifiedName()) || checkInterfaces(type, interfaceType.symbol())) {
        return true;
      }
    }
    return false;
  }

}
