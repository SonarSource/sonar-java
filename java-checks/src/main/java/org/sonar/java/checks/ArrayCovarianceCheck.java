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
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

@Rule(key = "S2330")
public class ArrayCovarianceCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Use the type of the actual array element here; array covariance can lead to ArrayStoreException at runtime.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE, Tree.Kind.ASSIGNMENT, Tree.Kind.RETURN_STATEMENT,
      Tree.Kind.YIELD_STATEMENT, Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case VARIABLE -> visitVariable((VariableTree) tree);
      case ASSIGNMENT -> {
        var assignment = (AssignmentExpressionTree) tree;
        checkArrayCovariance(assignment.variable().symbolType(), assignment.expression());
      }
      case RETURN_STATEMENT -> visitReturnStatement((ReturnStatementTree) tree);
      case YIELD_STATEMENT -> visitYieldStatement((YieldStatementTree) tree);
      case METHOD_INVOCATION -> {
        var invocation = (MethodInvocationTree) tree;
        visitInvocation(invocation.methodSymbol(), invocation.arguments());
      }
      case NEW_CLASS -> {
        var invocation = (NewClassTree) tree;
        visitInvocation(invocation.methodSymbol(), invocation.arguments());
      }
      default -> {
        // do nothing
      }
    }
  }

  private void visitVariable(VariableTree tree) {
    var initializer = tree.initializer();
    if (initializer != null) {
      checkArrayCovariance(tree.symbol().type(), initializer);
    }
  }

  private void visitReturnStatement(ReturnStatementTree tree) {
    var expression = tree.expression();
    if (expression == null) {
      return;
    }
    Tree enclosing = ExpressionUtils.getEnclosingTree(tree, Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
    if (enclosing != null) {
      var lhsType = enclosing instanceof LambdaExpressionTree lambda
        ? lambda.symbol().returnType().type()
        : ((MethodTree) enclosing).returnType().symbolType();
      checkArrayCovariance(lhsType, expression);
    }
  }

  private void visitYieldStatement(YieldStatementTree tree) {
    Tree enclosing = ExpressionUtils.getEnclosingTree(tree, Tree.Kind.SWITCH_EXPRESSION, Tree.Kind.SWITCH_STATEMENT);
    if (enclosing == null || enclosing.is(Tree.Kind.SWITCH_STATEMENT)) {
      return;
    }
    var lhsType = ((ExpressionTree) enclosing).symbolType();
    checkArrayCovariance(lhsType, tree.expression());
  }

  private void visitInvocation(Symbol.MethodSymbol methodSymbol, Arguments arguments) {
    List<Type> parameterTypes = methodSymbol.parameterTypes();
    var nonVarargCount = parameterTypes.size() - (methodSymbol.isVarArgsMethod() ? 1 : 0);
    for (int i = 0; i < nonVarargCount && i < arguments.size(); i++) {
      checkArrayCovariance(parameterTypes.get(i), arguments.get(i));
    }
    if (!methodSymbol.isVarArgsMethod() || arguments.size() == nonVarargCount) {
      return;
    }
    var varargType = (Type.ArrayType) parameterTypes.get(nonVarargCount);
    checkArrayCovariance(varargType, arguments.get(nonVarargCount));
    var elementType = varargType.elementType();
    for (int i = nonVarargCount; i < arguments.size(); i++) {
      checkArrayCovariance(elementType, arguments.get(i));
    }
  }

  private void checkArrayCovariance(Type lhsType, ExpressionTree rhsExpression) {
    var rhsType = rhsExpression.symbolType();
    if (!lhsType.isArray() || !rhsType.isArray() || rhsType.isNullType() || rhsType.isUnknown() || lhsType.isUnknown()) {
      return;
    }
    var lhsElementType = ((Type.ArrayType) lhsType).elementType();
    var rhsElementType = ((Type.ArrayType) rhsType).elementType();
    if (lhsElementType.isUnknown() || rhsElementType.isUnknown() || lhsElementType.isPrimitive() || rhsElementType.isPrimitive()) {
      return;
    }
    if (rhsElementType.isSubtypeOf(lhsElementType) && !lhsElementType.isSubtypeOf(rhsElementType)) {
      context.reportIssue(this, rhsExpression, MESSAGE);
    }
  }
}
