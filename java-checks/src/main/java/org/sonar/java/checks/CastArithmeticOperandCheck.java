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
import com.google.common.collect.ImmutableMap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Map;

@Rule(
  key = "S2184",
  name = "Math operands should be cast before assignment",
  tags = {"bug", "cwe", "sans-top25"},
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
        varType = ((AbstractTypedTree) aet.variable()).getSymbolType();
        expr = aet.expression();
        checkExpression(varType, expr);
      } else if (tree.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) tree;
        varType = ((AbstractTypedTree) variableTree.type()).getSymbolType();
        expr = variableTree.initializer();
        checkExpression(varType, expr);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        checkMethodInvocationArgument((MethodInvocationTreeImpl) tree);
      } else if (tree.is(Tree.Kind.METHOD)) {
        MethodTreeImpl methodTree = (MethodTreeImpl) tree;
        Type returnType = methodTree.getSymbol().getReturnType().getType();
        if (isVarTypeErrorProne(returnType)) {
          methodTree.accept(new ReturnStatementVisitor(returnType));
        }
      }
    }
  }

  private void checkMethodInvocationArgument(MethodInvocationTreeImpl mit) {
    Symbol symbol = mit.getSymbol();
    if (symbol.isKind(Symbol.MTH)) {
      List<Type> parametersTypes = ((Symbol.MethodSymbol) symbol).getParametersTypes();
      if (mit.arguments().size() == parametersTypes.size()) {
        int i = 0;
        for (Type argType : parametersTypes) {
          checkExpression(argType, mit.arguments().get(i));
          i++;
        }
      }
    }
  }

  private void checkExpression(Type varType, ExpressionTree expr) {
    if (expr != null && expr.is(Tree.Kind.MULTIPLY, Tree.Kind.DIVIDE, Tree.Kind.PLUS, Tree.Kind.MINUS) && isVarTypeErrorProne(varType)) {
      Type exprType = ((AbstractTypedTree) expr).getSymbolType();
      if (exprType.isTagged(Type.INT)) {
        addIssue(expr, "Cast one of the operands of this " + OPERATION_BY_KIND.get(((JavaTree) expr).getKind()) + " operation to a \"" + varType.getSymbol().getName() + "\".");
      }
    }
  }

  private boolean isVarTypeErrorProne(Type varType) {
    return varType.isTagged(Type.LONG) || varType.isTagged(Type.FLOAT) || varType.isTagged(Type.DOUBLE);
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

