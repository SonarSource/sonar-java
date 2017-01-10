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

import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1698")
public class CompareObjectWithEqualsCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (!isEquals(tree)) {
      super.visitMethod(tree);
    }
  }

  private static boolean isEquals(MethodTree tree) {
    return ((MethodTreeImpl) tree).isEqualsMethod();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      Type leftOpType = tree.leftOperand().symbolType();
      Type rightOpType = tree.rightOperand().symbolType();
      if (!isExcluded(leftOpType, rightOpType) && hasObjectOperand(leftOpType, rightOpType)) {
        context.reportIssue(this, tree.operatorToken(), "Change this comparison to use the equals method.");
      }
    }
  }

  private static boolean hasObjectOperand(Type leftOpType, Type rightOpType) {
    return isObject(leftOpType) || isObject(rightOpType);
  }

  private static boolean isExcluded(Type leftOpType, Type rightOpType) {
    return isNullComparison(leftOpType, rightOpType) || isNumericalComparison(leftOpType, rightOpType) || isJavaLangClassComparison(leftOpType, rightOpType);
  }

  private static boolean isObject(Type operandType) {
    return operandType.erasure().isClass() && !operandType.symbol().isEnum();
  }

  private static boolean isNullComparison(Type leftOpType, Type rightOpType) {
    return isBot(leftOpType) || isBot(rightOpType);
  }

  private static boolean isNumericalComparison(Type leftOperandType, Type rightOperandType) {
    return leftOperandType.isNumerical() || rightOperandType.isNumerical();
  }

  private static boolean isJavaLangClassComparison(Type leftOpType, Type rightOpType) {
    return leftOpType.is("java.lang.Class") || rightOpType.is("java.lang.Class");
  }

  private static boolean isBot(Type type) {
    return ((JavaType) type).isTagged(JavaType.BOT);

  }
}
