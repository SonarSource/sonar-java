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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9395")
public class S9395Check extends BaseTreeVisitor implements JavaFileScanner {

  private static final long FLOAT_MAX_EXACT_INT = 1L << 24;
  private static final long DOUBLE_MAX_EXACT_LONG = 1L << 53;

  private JavaFileScannerContext context;
  private final Deque<Type> floatingPointReturnTypes = new LinkedList<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    floatingPointReturnTypes.clear();
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitVariable(VariableTree tree) {
    checkExpression(tree.type().symbolType(), tree.initializer());
    super.visitVariable(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    Type targetType = tree.variable().symbolType();
    ExpressionTree rhs = tree.expression();
    checkExpression(targetType, rhs);
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    checkArguments(tree.arguments(), tree.methodSymbol());
    super.visitMethodInvocation(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    checkArguments(tree.arguments(), tree.methodSymbol());
    super.visitNewClass(tree);
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    Type returnType = floatingPointReturnTypes.peek();
    if (returnType != null) {
      checkExpression(returnType, tree.expression());
    }
    super.visitReturnStatement(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    TypeTree returnTypeTree = tree.returnType();
    Type returnType = returnTypeTree != null ? returnTypeTree.symbolType() : null;
    floatingPointReturnTypes.push(returnType != null && isFloatingPoint(returnType) ? returnType : null);
    super.visitMethod(tree);
    floatingPointReturnTypes.pop();
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree tree) {
    floatingPointReturnTypes.push(null);
    super.visitLambdaExpression(tree);
    floatingPointReturnTypes.pop();
  }

  private void checkArguments(Arguments arguments, Symbol.MethodSymbol symbol) {
    if (!symbol.isUnknown()) {
      List<Type> parameterTypes = symbol.parameterTypes();
      int fixedCount;
      if (symbol.isVarArgsMethod()) {
        fixedCount = parameterTypes.size() - 1;
      } else if (arguments.size() == parameterTypes.size()) {
        fixedCount = parameterTypes.size();
      } else {
        return;
      }
      for (int i = 0; i < fixedCount && i < arguments.size(); i++) {
        checkExpression(parameterTypes.get(i), arguments.get(i));
      }
    }
  }

  private void checkExpression(Type targetType, @Nullable ExpressionTree expr) {
    if (expr == null) {
      return;
    }
    ExpressionTree unwrapped = ExpressionUtils.skipParentheses(expr);
    if (unwrapped.is(Tree.Kind.TYPE_CAST)) {
      return;
    }
    Type sourceType = unwrapped.symbolType();
    if (isLossyWideningConversion(sourceType, targetType) && !isSafeConstant(unwrapped, targetType)) {
      context.reportIssue(this, unwrapped,
        "Explicitly cast this \"" + sourceType.name() + "\" to \"" + targetType.name() + "\" to document potential precision loss.");
    }
  }

  private static boolean isLossyWideningConversion(Type sourceType, Type targetType) {
    if (sourceType.isUnknown() || targetType.isUnknown()) {
      return false;
    }
    if (sourceType.isPrimitive(Type.Primitives.INT) && targetType.isPrimitive(Type.Primitives.FLOAT)) {
      return true;
    }
    if (sourceType.isPrimitive(Type.Primitives.LONG) && targetType.isPrimitive(Type.Primitives.FLOAT)) {
      return true;
    }
    return sourceType.isPrimitive(Type.Primitives.LONG) && targetType.isPrimitive(Type.Primitives.DOUBLE);
  }

  private static boolean isSafeConstant(ExpressionTree expr, Type targetType) {
    Object constant = ExpressionUtils.resolveAsConstant(expr);
    if (constant instanceof Integer intVal) {
      long absValue = Math.abs((long) intVal);
      return !targetType.isPrimitive(Type.Primitives.FLOAT) || absValue <= FLOAT_MAX_EXACT_INT;
    }
    if (constant instanceof Long longVal) {
      long absValue = Math.abs(longVal);
      if (targetType.isPrimitive(Type.Primitives.FLOAT)) {
        return absValue <= FLOAT_MAX_EXACT_INT;
      }
      if (targetType.isPrimitive(Type.Primitives.DOUBLE)) {
        return absValue <= DOUBLE_MAX_EXACT_LONG;
      }
    }
    return false;
  }

  private static boolean isFloatingPoint(Type type) {
    return type.isPrimitive(Type.Primitives.FLOAT) || type.isPrimitive(Type.Primitives.DOUBLE);
  }

}
