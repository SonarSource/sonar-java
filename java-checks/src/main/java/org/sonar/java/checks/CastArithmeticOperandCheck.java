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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableMap;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

@Rule(key = "S2184")
public class CastArithmeticOperandCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Map<Tree.Kind, String> OPERATION_BY_KIND = ImmutableMap.<Tree.Kind, String>builder()
    .put(Tree.Kind.PLUS, "addition")
    .put(Tree.Kind.MINUS, "subtraction")
    .put(Tree.Kind.MULTIPLY, "multiplication")
    .put(Tree.Kind.DIVIDE, "division")
    .build();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if(context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree aet) {
    if(aet.is(Tree.Kind.ASSIGNMENT)) {
      Type varType = aet.symbolType();
      ExpressionTree expr = aet.expression();
      checkExpression(varType, expr);
    }
    super.visitAssignmentExpression(aet);
  }

  @Override
  public void visitVariable(VariableTree tree) {
    Type varType = tree.type().symbolType();
    checkExpression(varType, tree.initializer());
    super.visitVariable(tree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    checkMethodInvocationArgument(tree.arguments(), tree.symbol());
    super.visitMethodInvocation(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      checkMethodTree(tree);
    }
    super.visitMethod(tree);
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    boolean continueVisit = true;
    if (tree.is(Tree.Kind.DIVIDE) && isIntOrLong(tree.symbolType())) {
      continueVisit = checkIntegerDivisionInsideFloatingPointExpression(tree);
    }
    if (continueVisit) {
      super.visitBinaryExpression(tree);
    }
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    checkMethodInvocationArgument(tree.arguments(), tree.constructorSymbol());
    super.visitNewClass(tree);
  }

  private void checkMethodTree(MethodTree methodTree) {
    TypeTree returnTypeTree = methodTree.returnType();
    Type returnType = returnTypeTree != null ? returnTypeTree.symbolType() : null;
    if (returnType != null && isVarTypeErrorProne(returnType)) {
      methodTree.accept(new ReturnStatementVisitor(returnType));
    }
  }

  private void checkMethodInvocationArgument(Arguments arguments, Symbol symbol) {
    if (symbol.isMethodSymbol()) {
      List<Type> parametersTypes = ((Symbol.MethodSymbol) symbol).parameterTypes();
      if (arguments.size() == parametersTypes.size()) {
        int i = 0;
        for (Type argType : parametersTypes) {
          checkExpression(argType, arguments.get(i));
          i++;
        }
      }
    }
  }

  private void checkExpression(Type varType, @Nullable ExpressionTree expr) {
    if (isVarTypeErrorProne(varType) && expr != null && expressionIsOperationToIntOrLong(expr)) {
      BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) expr;
      if(binaryExpressionTree.is(Tree.Kind.DIVIDE) && varType.isPrimitive(Type.Primitives.LONG)) {
        // widening the result of an int division is harmless
        return;
      }
      if (varType.isPrimitive(Type.Primitives.LONG) && expr.symbolType().isPrimitive(Type.Primitives.LONG)) {
        return;
      }
      context.reportIssue(this,
        binaryExpressionTree.operatorToken(), "Cast one of the operands of this " + OPERATION_BY_KIND.get(expr.kind()) + " operation to a \"" + varType.name() + "\".");
    }
  }

  private static boolean expressionIsOperationToIntOrLong(ExpressionTree expr) {
    return expr.is(Tree.Kind.MULTIPLY, Tree.Kind.DIVIDE, Tree.Kind.PLUS, Tree.Kind.MINUS) && isIntOrLong(expr.symbolType());
  }

  private static boolean isIntOrLong(Type exprType) {
    return exprType.isPrimitive(Type.Primitives.INT) || exprType.isPrimitive(Type.Primitives.LONG);
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

  private boolean checkIntegerDivisionInsideFloatingPointExpression(BinaryExpressionTree integerDivision) {
    Tree parent = integerDivision.parent();
    while (parent instanceof ExpressionTree) {
      ExpressionTree expressionTree = (ExpressionTree) parent;
      if (isFloatingPoint(expressionTree.symbolType())) {
        context.reportIssue(this, integerDivision, "Cast one of the operands of this integer division to a \"double\".");
        return false;
      }
      parent = expressionTree.parent();
    }
    return true;
  }

  private static boolean isFloatingPoint(Type exprType) {
    return exprType.isPrimitive(Type.Primitives.DOUBLE) || exprType.isPrimitive(Type.Primitives.FLOAT);
  }
}
