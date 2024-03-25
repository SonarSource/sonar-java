/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.text.MessageFormat;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

@Rule(key = "S2438")
public class ThreadAsRunnableArgumentCheck extends IssuableSubscriptionVisitor {

  private static final String RUNNABLE_TYPE = "java.lang.Runnable";
  private static final String THREAD_TYPE = "java.lang.Thread";
  private static final String RUNNABLE_ARRAY_TYPE = RUNNABLE_TYPE + "[]";
  private static final String THREAD_ARRAY_TYPE = THREAD_TYPE + "[]";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE, Tree.Kind.RETURN_STATEMENT, Tree.Kind.YIELD_STATEMENT, Tree.Kind.ASSIGNMENT,
      Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS, Tree.Kind.NEW_ARRAY);
  }

  @Override
  @SuppressWarnings("squid:S131")
  public void visitNode(Tree tree) {
    switch(tree.kind()) {
      case VARIABLE -> visitVariable((VariableTree) tree);
      case RETURN_STATEMENT -> visitReturnStatement((ReturnStatementTree) tree);
      case YIELD_STATEMENT -> visitYieldStatement((YieldStatementTree) tree);
      case NEW_ARRAY -> visitNewArray((NewArrayTree) tree);
      case ASSIGNMENT -> {
        var assignment = (AssignmentExpressionTree) tree;
        checkTypeCoercion(assignment.variable().symbolType(), assignment.expression());
      }
      case METHOD_INVOCATION -> {
        var invocation = (MethodInvocationTree) tree;
        visitInvocation(invocation.methodSymbol(), invocation.arguments());
      }
      case NEW_CLASS -> {
        var invocation = (NewClassTree) tree;
        visitInvocation(invocation.methodSymbol(), invocation.arguments());
      }
    }
  }

  private void visitVariable(VariableTree tree) {
    var initializer = tree.initializer();
    if (initializer != null) {
      checkTypeCoercion(tree.symbol().type(), initializer);
    }
  }

  private void visitInvocation(Symbol.MethodSymbol methodSymbol, Arguments rhsValues) {
    List<Type> lhsTypes = methodSymbol.parameterTypes();

    var nonVarargCount = lhsTypes.size() - (methodSymbol.isVarArgsMethod() ? 1 : 0);
    for (int i = 0; i < nonVarargCount; i++) {
      checkTypeCoercion(lhsTypes.get(i), rhsValues.get(i));
    }
    var argumentCount = rhsValues.size();
    if (!methodSymbol.isVarArgsMethod() || argumentCount == nonVarargCount) {
      return;
    }

    var arrayType = (Type.ArrayType) lhsTypes.get(nonVarargCount);
    var elementType = arrayType.elementType();
    checkTypeCoercion(arrayType, rhsValues.get(nonVarargCount));
    for (int i = nonVarargCount; i < argumentCount; i++) {
      checkTypeCoercion(elementType, rhsValues.get(i));
    }
  }

  private void visitNewArray(NewArrayTree tree) {
    var lhsType = tree.type();
    if (lhsType != null && lhsType.symbolType().is(RUNNABLE_TYPE)) {
      tree.initializers().forEach(rhsValue -> checkTypeCoercion(lhsType.symbolType(), rhsValue));
    }
  }

  private void visitReturnStatement(ReturnStatementTree tree) {
    var expression = tree.expression();
    if (expression == null) {
      return;
    }

    Tree enclosing = ExpressionUtils.getEnclosingTree(tree, Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
    if (enclosing != null) {
      var lhsType = enclosing instanceof LambdaExpressionTree lambda ?
        lambda.symbol().returnType().type() : ((MethodTree) enclosing).returnType().symbolType();
      checkTypeCoercion(lhsType, expression);
    }
  }

  private void visitYieldStatement(YieldStatementTree tree) {
    Tree enclosing = ExpressionUtils.getEnclosingTree(tree, Tree.Kind.SWITCH_EXPRESSION, Tree.Kind.SWITCH_STATEMENT);
    if (enclosing == null || enclosing.is(Tree.Kind.SWITCH_STATEMENT)) {
      // Iside of a SwitchStatementTree, the `yield` is implicit and does not return a value.
      // Unlike SwitchExpressionTree, SwitchStatementTree is not an ExpressionTree.
      return;
    }
    var lhsType = ((ExpressionTree) enclosing).symbolType();
    checkTypeCoercion(lhsType, tree.expression());
  }

  private void checkTypeCoercion(Type lhsType, ExpressionTree rhsValue) {
    var rhsType = rhsValue.symbolType();
    if ((lhsType.is(RUNNABLE_TYPE) && isNonNullSubtypeOf(rhsType, THREAD_TYPE)) ||
      (lhsType.is(RUNNABLE_ARRAY_TYPE) && isNonNullSubtypeOf(rhsType, THREAD_ARRAY_TYPE))) {
      var message = MessageFormat.format("Replace this {0} instance with an instance of {1}.", rhsType.name(), lhsType.name());
      context.reportIssue(this, rhsValue, message);
    }
  }

  private static boolean isNonNullSubtypeOf(Type type, String superTypeName) {
    return !type.isNullType() && type.isSubtypeOf(superTypeName);
  }
}
