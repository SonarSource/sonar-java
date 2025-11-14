/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3034")
public class RawByteBitwiseOperationsCheck extends BaseTreeVisitor implements JavaFileScanner {

  JavaFileScannerContext context;
  Set<ExpressionTree> shifts = new HashSet<>();
  Set<ExpressionTree> byteContainments = new HashSet<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
    shifts.clear();
    byteContainments.clear();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (isShifting(tree)) {
      shifts.add(tree);
      return;
    }
    if (ExpressionUtils.isSecuringByte(tree)) {
      byteContainments.add(tree);
      return;
    }
    if (isIntegerOrLongExpected(tree.symbolType())) {
      ExpressionTree leftOperand = ExpressionUtils.skipParentheses(tree.leftOperand());
      ExpressionTree rightOperand = ExpressionUtils.skipParentheses(tree.rightOperand());
      checkShiftWithoutByteSecuring(leftOperand, rightOperand);
      checkShiftWithoutByteSecuring(rightOperand, leftOperand);
    }
  }

  private static boolean isShifting(BinaryExpressionTree tree) {
    return tree.is(Tree.Kind.LEFT_SHIFT, Tree.Kind.RIGHT_SHIFT, Tree.Kind.UNSIGNED_RIGHT_SHIFT);
  }

  private static boolean isIntegerOrLongExpected(Type type) {
    return type.isPrimitive(Primitives.INT) || type.isPrimitive(Primitives.LONG);
  }

  private void checkShiftWithoutByteSecuring(ExpressionTree shiftExpr, ExpressionTree byteExpr) {
    if (shifts.contains(shiftExpr) && !byteContainments.contains(byteExpr) && byteExpr.symbolType().isPrimitive(Primitives.BYTE)) {
      context.reportIssue(this, byteExpr, "Prevent \"int\" promotion by adding \"& 0xff\" to this expression.");
    }
  }

}
