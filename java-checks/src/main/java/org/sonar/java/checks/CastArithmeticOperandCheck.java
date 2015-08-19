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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2184",
  name = "Math operands should be cast before assignment",
  tags = {"bug", "cwe", "sans-top25-risky", "security"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class CastArithmeticOperandCheck extends SubscriptionBaseVisitor {

  private static final Map<Tree.Kind, String> OPERATION_BY_KIND = ImmutableMap.<Tree.Kind, String>builder()
    .put(Tree.Kind.PLUS, "addition")
    .put(Tree.Kind.MINUS, "substraction")
    .put(Tree.Kind.MULTIPLY, "multiplication")
    .put(Tree.Kind.DIVIDE, "division")
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ASSIGNMENT, Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      Type varType;
      ExpressionTree expr;
      if (tree.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree aet = (AssignmentExpressionTree) tree;
        varType = aet.symbolType();
        expr = aet.expression();
        checkExpression(varType, expr);
      } else if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        varType = variableTree.type().symbolType();
        expr = variableTree.initializer();
        checkExpression(varType, expr);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        checkMethodInvocationArgument((MethodInvocationTree) tree);
      } else if (tree.is(Tree.Kind.METHOD)) {
        MethodTreeImpl methodTree = (MethodTreeImpl) tree;
        Type returnType = methodTree.returnType() != null ? methodTree.returnType().symbolType() : null;
        if (returnType != null && isVarTypeErrorProne(returnType)) {
          methodTree.accept(new ReturnStatementVisitor(returnType));
        }
      }
    }
  }

  private void checkMethodInvocationArgument(MethodInvocationTree mit) {
    Symbol symbol = mit.symbol();
    if (symbol.isMethodSymbol()) {
      List<Type> parametersTypes = ((Symbol.MethodSymbol) symbol).parameterTypes();
      if (mit.arguments().size() == parametersTypes.size()) {
        int i = 0;
        for (Type argType : parametersTypes) {
          checkExpression(argType, mit.arguments().get(i));
          i++;
        }
      }
    }
  }

  private void checkExpression(Type varType, @Nullable ExpressionTree expr) {
    if (isVarTypeErrorProne(varType) && expressionIsOperationToInt(expr)) {
      addIssue(expr, "Cast one of the operands of this " + OPERATION_BY_KIND.get(expr.kind()) + " operation to a \"" + varType.name() + "\".");
    }
  }

  private static boolean expressionIsOperationToInt(@Nullable ExpressionTree expr) {
    return expr != null && expr.is(Tree.Kind.MULTIPLY, Tree.Kind.DIVIDE, Tree.Kind.PLUS, Tree.Kind.MINUS)
      && expr.symbolType().isPrimitive(org.sonar.plugins.java.api.semantic.Type.Primitives.INT);
  }

  private static boolean isVarTypeErrorProne(Type varType) {
    return varType.isPrimitive(Type.Primitives.LONG) || varType.isPrimitive(Type.Primitives.FLOAT) || varType.isPrimitive(Type.Primitives.DOUBLE);
  }

  private class ReturnStatementVisitor extends BaseTreeVisitor {
    private Type returnType;

    public ReturnStatementVisitor(Type returnType) {
      this.returnType = returnType;
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      checkExpression(returnType, tree.expression());
    }
  }
}
